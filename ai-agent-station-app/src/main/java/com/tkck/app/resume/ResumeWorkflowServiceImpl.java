package com.tkck.app.resume;

import com.tkck.config.AiAgentConfig;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.model.valobj.enums.AiAgentEnumVO;
import com.tkck.domain.agent.service.execute.auto.step.factory.DefaultAutoAgentExecuteStrategyFactory;
import com.tkck.domain.resume.model.entity.ResumeInterviewDetailEntity;
import com.tkck.domain.resume.model.entity.ResumeInterviewRoundEntity;
import com.tkck.domain.resume.model.entity.ResumeInterviewStartEntity;
import com.tkck.domain.resume.model.entity.ResumeUploadResultEntity;
import com.tkck.domain.resume.service.IResumeWorkflowService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeWorkflowServiceImpl implements IResumeWorkflowService {

    private static final int FIXED_INTERVIEW_TOTAL_ROUNDS = 3;
    private static final int MAX_SCORE_LENGTH = 32;
    private static final Pattern SCORE_TOKEN_PATTERN =
            Pattern.compile("(?i)(\\d{1,2}(?:\\.\\d)?\\s*/\\s*10|\\d{1,3}%|\\d{1,2}(?:\\.\\d)?)");

    @Resource(name = "mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbcTemplate;

    @Resource(name = "vectorStore")
    private VectorStore vectorStore;

    @Resource(name = "jobStandardVectorStore")
    private VectorStore jobStandardVectorStore;

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private ApplicationContext applicationContext;

    @Override
    public ResumeUploadResultEntity uploadResume(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("resume file is empty");
        }

        byte[] bytes = file.getBytes();
        String fileName = file.getOriginalFilename() == null ? "resume.pdf" : file.getOriginalFilename();
        String rawText = readResumeText(bytes, fileName);
        Long resumeId = insertResumeProfile(fileName, bytes, rawText);
        Long knowledgeSpaceId = insertKnowledgeSpace(resumeId);

        List<Document> splitDocuments = tokenTextSplitter.apply(List.of(new Document(rawText)));
        for (int i = 0; i < splitDocuments.size(); i++) {
            Document document = splitDocuments.get(i);
            Map<String, Object> metadata = ResumeMetadataSupport.buildChunkMetadata(knowledgeSpaceId, resumeId, fileName, i);
            metadata.forEach(document.getMetadata()::put);
        }
        vectorStore.accept(splitDocuments);

        mysqlJdbcTemplate.update(
                "UPDATE resume_knowledge_space SET chunk_count = ?, update_time = NOW() WHERE id = ?",
                splitDocuments.size(), knowledgeSpaceId);

        return ResumeUploadResultEntity.builder()
                .resumeId(resumeId)
                .knowledgeSpaceId(knowledgeSpaceId)
                .knowledgeTag(ResumeMetadataSupport.KNOWLEDGE_TAG)
                .fileName(fileName)
                .chunkCount(splitDocuments.size())
                .build();
    }

    @Override
    public ExecuteCommandEntity buildResumeEvaluationCommand(Long resumeId, Long knowledgeSpaceId, String question, String sessionId, Integer maxStep) {
        String resumeText = mysqlJdbcTemplate.queryForObject(
                "SELECT raw_text FROM resume_profile WHERE id = ?",
                String.class,
                resumeId);
        String jobCode = "java_backend";
        String jobStandardContext = retrieveJobStandardContext(jobCode,
                (question == null || question.isBlank() ? "请评估这份简历与 Java 后端岗位的匹配度" : question)
                        + "\n简历摘要:\n" + truncate(resumeText, 1200));

        return ExecuteCommandEntity.builder()
                .aiAgentId("1001")
                .sessionId(sessionId == null || sessionId.isBlank() ? "resume-eval-" + knowledgeSpaceId : sessionId)
                .maxStep(maxStep == null ? 3 : maxStep)
                .qaFilterExpression(ResumeMetadataSupport.buildKnowledgeFilterExpression(knowledgeSpaceId))
                .message(ResumeWorkflowPromptBuilder.buildEvaluationMessage(resumeId, knowledgeSpaceId, question)
                        + "\n\n岗位标准参考(jobCode=" + jobCode + "):\n"
                        + jobStandardContext
                        + "\n\n额外要求:\n"
                        + "1. 结论必须同时参考候选人简历片段与岗位标准片段。\n"
                        + "2. 明确指出命中的技能标准、弱匹配项和缺失项。\n"
                        + "3. 如果岗位标准片段未覆盖某结论，不能强行下判断。")
                .build();
    }

    @Override
    public ResumeInterviewStartEntity startInterview(Long resumeId, Long knowledgeSpaceId) {
        String resumeText = mysqlJdbcTemplate.queryForObject(
                "SELECT raw_text FROM resume_profile WHERE id = ?",
                String.class,
                resumeId);
        ChatClient chatClient = getChatClient("5201");
        String openingQuestions = chatClient.prompt(
                        ResumeWorkflowPromptBuilder.buildInterviewOpeningPrompt(
                                resumeId,
                                knowledgeSpaceId,
                                truncate(resumeText, 3000)))
                .call()
                .content();
        List<String> openingQuestionList = parseQuestionList(openingQuestions);
        String firstQuestion = openingQuestionList.isEmpty() ? openingQuestions : openingQuestionList.get(0);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String sessionCode = "interview-" + UUID.randomUUID();
        mysqlJdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO resume_interview_session (resume_id, knowledge_space_id, session_code, opening_questions, current_round, total_rounds, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, resumeId);
            ps.setLong(2, knowledgeSpaceId);
            ps.setString(3, sessionCode);
            ps.setString(4, openingQuestions);
            ps.setInt(5, 1);
            ps.setInt(6, FIXED_INTERVIEW_TOTAL_ROUNDS);
            ps.setString(7, "STARTED");
            return ps;
        }, keyHolder);
        Long interviewSessionId = keyHolder.getKey().longValue();

        mysqlJdbcTemplate.update(
                "INSERT INTO resume_interview_round (interview_session_id, round_no, question_content, status) VALUES (?, ?, ?, ?)",
                interviewSessionId, 1, firstQuestion, "ASKED");

        return ResumeInterviewStartEntity.builder()
                .interviewSessionId(interviewSessionId)
                .currentRound(1)
                .totalRounds(FIXED_INTERVIEW_TOTAL_ROUNDS)
                .status("STARTED")
                .openingQuestions(openingQuestions)
                .build();
    }

    @Override
    public ExecuteCommandEntity buildInterviewAnswerCommand(Long interviewSessionId, Integer roundNo, String answer, String sessionId, Integer maxStep) {
        Map<String, Object> sessionRow = mysqlJdbcTemplate.queryForMap(
                "SELECT id, knowledge_space_id, current_round, total_rounds, status FROM resume_interview_session WHERE id = ?",
                interviewSessionId);
        String status = (String) sessionRow.get("status");
        if ("FINISHED".equalsIgnoreCase(status)) {
            throw new IllegalStateException("当前面试已结束，不能继续作答。interviewSessionId=" + interviewSessionId);
        }

        Integer currentRound = ((Number) sessionRow.get("current_round")).intValue();
        Integer actualRound = roundNo == null ? currentRound : roundNo;
        if (!actualRound.equals(currentRound)) {
            throw new IllegalStateException("当前轮次与会话进度不一致，请先刷新面试详情。interviewSessionId="
                    + interviewSessionId + ", expectedRound=" + currentRound + ", actualRound=" + actualRound);
        }

        Integer totalRounds = sessionRow.get("total_rounds") == null
                ? FIXED_INTERVIEW_TOTAL_ROUNDS
                : ((Number) sessionRow.get("total_rounds")).intValue();
        Long knowledgeSpaceId = ((Number) sessionRow.get("knowledge_space_id")).longValue();

        List<String> questionRows = mysqlJdbcTemplate.queryForList(
                "SELECT question_content FROM resume_interview_round WHERE interview_session_id = ? AND round_no = ?",
                String.class,
                interviewSessionId,
                actualRound);
        if (questionRows.isEmpty()) {
            throw new IllegalStateException("当前轮次不存在，请先刷新面试详情后再继续。interviewSessionId="
                    + interviewSessionId + ", roundNo=" + actualRound);
        }
        String currentQuestion = questionRows.get(0);

        mysqlJdbcTemplate.update(
                "UPDATE resume_interview_round SET answer_content = ?, status = ?, update_time = NOW() WHERE interview_session_id = ? AND round_no = ?",
                answer, "ANSWERED", interviewSessionId, actualRound);
        mysqlJdbcTemplate.update(
                "UPDATE resume_interview_session SET status = ?, update_time = NOW() WHERE id = ?",
                "IN_PROGRESS", interviewSessionId);

        return ExecuteCommandEntity.builder()
                .aiAgentId("1002")
                .sessionId(sessionId == null || sessionId.isBlank() ? "resume-interview-" + interviewSessionId : sessionId)
                .maxStep(maxStep == null ? 3 : maxStep)
                .qaFilterExpression(ResumeMetadataSupport.buildKnowledgeFilterExpression(knowledgeSpaceId))
                .interviewSessionId(interviewSessionId)
                .interviewRoundNo(actualRound)
                .interviewTotalRounds(totalRounds)
                .message(ResumeWorkflowPromptBuilder.buildInterviewAnswerMessage(
                        interviewSessionId,
                        actualRound,
                        totalRounds,
                        currentQuestion,
                        answer))
                .build();
    }

    @Override
    public void persistInterviewRoundResult(ExecuteCommandEntity executeCommandEntity,
                                            DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) {
        if (executeCommandEntity.getInterviewSessionId() == null || executeCommandEntity.getInterviewRoundNo() == null) {
            return;
        }

        Long interviewSessionId = executeCommandEntity.getInterviewSessionId();
        Integer roundNo = executeCommandEntity.getInterviewRoundNo();
        Integer totalRounds = executeCommandEntity.getInterviewTotalRounds() == null
                ? FIXED_INTERVIEW_TOTAL_ROUNDS
                : executeCommandEntity.getInterviewTotalRounds();
        boolean finalRound = roundNo >= totalRounds;

        String finalSummary = dynamicContext.getValue("finalSummary");
        if (finalSummary == null || finalSummary.isBlank()) {
            finalSummary = dynamicContext.getValue("executionResult");
        }
        if (finalSummary == null) {
            finalSummary = "";
        }

        ResumeInterviewStructuredResult structuredResult = ResumeInterviewStructuredResultParser.parse(finalSummary, finalRound);
        String score = extractCompactScore(structuredResult.getScore() == null ? finalSummary : structuredResult.getScore());
        String nextQuestion = structuredResult.getNextQuestion();
        String finalReport = structuredResult.getFinalReport();

        mysqlJdbcTemplate.update(
                "UPDATE resume_interview_round SET feedback_content = ?, next_question = ?, score = ?, status = ?, update_time = NOW() WHERE interview_session_id = ? AND round_no = ?",
                finalSummary,
                finalRound ? null : truncate(nextQuestion, 500),
                score,
                "EVALUATED",
                interviewSessionId,
                roundNo);

        if (finalRound) {
            mysqlJdbcTemplate.update(
                    "UPDATE resume_interview_session SET current_round = ?, status = ?, final_report = ?, update_time = NOW() WHERE id = ?",
                    roundNo,
                    "FINISHED",
                    finalReport == null || finalReport.isBlank() ? finalSummary : finalReport,
                    interviewSessionId);
            return;
        }

        Integer nextRound = roundNo + 1;
        String actualNextQuestion = (nextQuestion == null || nextQuestion.isBlank())
                ? "请继续基于上一轮回答中的薄弱点，展开更深入的技术说明。"
                : nextQuestion;

        Integer exists = mysqlJdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM resume_interview_round WHERE interview_session_id = ? AND round_no = ?",
                Integer.class,
                interviewSessionId,
                nextRound);
        if (exists != null && exists == 0) {
            mysqlJdbcTemplate.update(
                    "INSERT INTO resume_interview_round (interview_session_id, round_no, question_content, status) VALUES (?, ?, ?, ?)",
                    interviewSessionId, nextRound, actualNextQuestion, "ASKED");
        } else {
            mysqlJdbcTemplate.update(
                    "UPDATE resume_interview_round SET question_content = ?, status = ?, update_time = NOW() WHERE interview_session_id = ? AND round_no = ?",
                    actualNextQuestion, "ASKED", interviewSessionId, nextRound);
        }

        mysqlJdbcTemplate.update(
                "UPDATE resume_interview_session SET current_round = ?, status = ?, update_time = NOW() WHERE id = ?",
                nextRound,
                "IN_PROGRESS",
                interviewSessionId);
    }

    @Override
    public ResumeInterviewDetailEntity queryInterviewDetail(Long interviewSessionId) {
        Map<String, Object> sessionRow = mysqlJdbcTemplate.queryForMap(
                "SELECT id, resume_id, knowledge_space_id, session_code, opening_questions, current_round, total_rounds, status, final_report FROM resume_interview_session WHERE id = ?",
                interviewSessionId);

        List<Map<String, Object>> roundRows = mysqlJdbcTemplate.queryForList(
                "SELECT round_no, question_content, answer_content, feedback_content, next_question, score, status FROM resume_interview_round WHERE interview_session_id = ? ORDER BY round_no ASC",
                interviewSessionId);

        List<ResumeInterviewRoundEntity> rounds = new ArrayList<>();
        int totalRounds = sessionRow.get("total_rounds") == null
                ? FIXED_INTERVIEW_TOTAL_ROUNDS
                : ((Number) sessionRow.get("total_rounds")).intValue();
        for (Map<String, Object> row : roundRows) {
            int roundNo = ((Number) row.get("round_no")).intValue();
            boolean finalRound = roundNo >= totalRounds;
            String feedbackContent = (String) row.get("feedback_content");
            ResumeInterviewStructuredResult parsed = ResumeInterviewStructuredResultParser.parse(feedbackContent, finalRound);
            rounds.add(ResumeInterviewRoundEntity.builder()
                    .roundNo(roundNo)
                    .questionContent((String) row.get("question_content"))
                    .answerContent((String) row.get("answer_content"))
                    .feedbackContent(feedbackContent)
                    .strengths(parsed.getStrengths())
                    .weaknesses(parsed.getWeaknesses())
                    .resumeEvidence(parsed.getResumeEvidence())
                    .followUpIntent(parsed.getFollowUpIntent())
                    .nextQuestion((String) row.get("next_question"))
                    .score((String) row.get("score"))
                    .finished(parsed.isFinished())
                    .status((String) row.get("status"))
                    .build());
        }

        return ResumeInterviewDetailEntity.builder()
                .interviewSessionId(((Number) sessionRow.get("id")).longValue())
                .resumeId(((Number) sessionRow.get("resume_id")).longValue())
                .knowledgeSpaceId(((Number) sessionRow.get("knowledge_space_id")).longValue())
                .sessionCode((String) sessionRow.get("session_code"))
                .openingQuestions((String) sessionRow.get("opening_questions"))
                .currentRound(((Number) sessionRow.get("current_round")).intValue())
                .totalRounds(totalRounds)
                .status((String) sessionRow.get("status"))
                .finalReport((String) sessionRow.get("final_report"))
                .rounds(rounds)
                .build();
    }

    private String readResumeText(byte[] bytes, String fileName) {
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.get();
        StringBuilder content = new StringBuilder();
        for (Document document : documents) {
            if (document.getText() != null) {
                content.append(document.getText()).append("\n");
            }
        }
        return content.toString();
    }

    private Long insertResumeProfile(String fileName, byte[] bytes, String rawText) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        mysqlJdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO resume_profile (file_name, file_hash, raw_text, status) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, fileName);
            ps.setString(2, DigestUtils.md5DigestAsHex(bytes));
            ps.setString(3, rawText);
            ps.setInt(4, 1);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private Long insertKnowledgeSpace(Long resumeId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        mysqlJdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO resume_knowledge_space (resume_id, knowledge_tag, vector_table, chunk_count, status) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, resumeId);
            ps.setString(2, ResumeMetadataSupport.KNOWLEDGE_TAG);
            ps.setString(3, AiAgentConfig.RESUME_VECTOR_TABLE);
            ps.setInt(4, 0);
            ps.setInt(5, 1);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private ChatClient getChatClient(String clientId) {
        return (ChatClient) applicationContext.getBean(AiAgentEnumVO.AI_CLIENT.getBeanName(clientId));
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String extractCompactScore(String text) {
        if (text == null || text.isBlank()) {
            return "N/A";
        }
        String firstLine = text.replace("\r\n", "\n").split("\n")[0].trim();
        Matcher matcher = SCORE_TOKEN_PATTERN.matcher(firstLine);
        if (matcher.find()) {
            return truncate(matcher.group(1).replaceAll("\\s+", ""), MAX_SCORE_LENGTH);
        }

        String compact = firstLine
                .replace("：", ":")
                .replaceAll("[*#>`-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return compact.isBlank() ? "N/A" : truncate(compact, MAX_SCORE_LENGTH);
    }

    private String retrieveJobStandardContext(String jobCode, String queryText) {
        List<Document> documents = jobStandardVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(truncate(queryText, 1500))
                        .topK(6)
                        .similarityThreshold(0.1)
                        .filterExpression(JobStandardMetadataSupport.buildJobFilterExpression(jobCode))
                        .build());
        if (documents == null || documents.isEmpty()) {
            return "未检索到岗位标准，请按 Java 后端通用标准谨慎评估。";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (Document document : documents) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(index++).append(". ").append(truncate(document.getText(), 280));
        }
        return builder.toString();
    }

    private List<String> parseQuestionList(String openingQuestions) {
        List<String> questions = new ArrayList<>();
        if (openingQuestions == null || openingQuestions.isBlank()) {
            return questions;
        }
        String[] lines = openingQuestions.replace("\r\n", "\n").split("\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            String normalized = line.replaceFirst("^[0-9]+[.、]\\s*", "")
                    .replaceFirst("^[-*]\\s*", "");
            if (!normalized.isBlank()) {
                questions.add(normalized);
            }
        }
        if (questions.isEmpty()) {
            questions.add(openingQuestions.trim());
        }
        return questions;
    }
}
