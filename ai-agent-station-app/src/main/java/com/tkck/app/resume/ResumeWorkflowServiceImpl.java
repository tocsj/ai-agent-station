package com.tkck.app.resume;

import com.tkck.config.AiAgentConfig;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.model.valobj.enums.AiAgentEnumVO;
import com.tkck.domain.agent.service.execute.auto.step.factory.DefaultAutoAgentExecuteStrategyFactory;
import com.tkck.domain.resume.adapter.repository.IResumeWorkflowRepository;
import com.tkck.domain.resume.model.entity.ResumeEvaluationTaskEntity;
import com.tkck.domain.resume.model.entity.ResumeInterviewDetailEntity;
import com.tkck.domain.resume.model.entity.ResumeInterviewRoundEntity;
import com.tkck.domain.resume.model.entity.ResumeInterviewStartEntity;
import com.tkck.domain.resume.model.entity.ResumeUploadResultEntity;
import com.tkck.domain.resume.service.IResumeWorkflowService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeWorkflowServiceImpl implements IResumeWorkflowService {

    private static final int DEFAULT_INTERVIEW_TOTAL_ROUNDS = 5;
    private static final int MAX_SCORE_LENGTH = 32;
    private static final Pattern SCORE_TOKEN_PATTERN =
            Pattern.compile("(?i)(\\d{1,2}(?:\\.\\d)?\\s*/\\s*10|\\d{1,3}%|\\d{1,2}(?:\\.\\d)?)");

    @Resource
    private IResumeWorkflowRepository resumeWorkflowRepository;
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
        Long resumeId = resumeWorkflowRepository.insertResumeProfile(fileName, DigestUtils.md5DigestAsHex(bytes), rawText);
        Long knowledgeSpaceId = resumeWorkflowRepository.insertKnowledgeSpace(
                resumeId,
                ResumeMetadataSupport.KNOWLEDGE_TAG,
                AiAgentConfig.RESUME_VECTOR_TABLE);

        List<Document> splitDocuments = tokenTextSplitter.apply(List.of(new Document(rawText)));
        for (int i = 0; i < splitDocuments.size(); i++) {
            Document document = splitDocuments.get(i);
            ResumeMetadataSupport.buildChunkMetadata(knowledgeSpaceId, resumeId, fileName, i)
                    .forEach(document.getMetadata()::put);
        }
        vectorStore.accept(splitDocuments);
        resumeWorkflowRepository.updateKnowledgeSpaceChunkCount(knowledgeSpaceId, splitDocuments.size());

        return ResumeUploadResultEntity.builder()
                .resumeId(resumeId)
                .knowledgeSpaceId(knowledgeSpaceId)
                .knowledgeTag(ResumeMetadataSupport.KNOWLEDGE_TAG)
                .fileName(fileName)
                .chunkCount(splitDocuments.size())
                .build();
    }

    @Override
    public List<ResumeUploadResultEntity> queryRecentResumes(Integer limit) {
        return resumeWorkflowRepository.queryRecentResumes(normalizeLimit(limit, 20, 50));
    }

    @Override
    public ExecuteCommandEntity buildResumeEvaluationCommand(Long resumeId, Long knowledgeSpaceId, String question, String sessionId, Integer maxStep) {
        String actualSessionId = sessionId == null || sessionId.isBlank() ? "resume-eval-" + knowledgeSpaceId : sessionId;
        String actualQuestion = question == null || question.isBlank() ? "evaluate resume" : question;
        String resumeText = resumeWorkflowRepository.queryResumeText(resumeId);
        String jobStandardContext = retrieveJobStandardContext("java_backend", actualQuestion + "\n" + truncate(resumeText, 1200));
        Long evaluationTaskId = resumeWorkflowRepository.insertResumeEvaluationTask(resumeId, knowledgeSpaceId, actualSessionId, actualQuestion);

        return ExecuteCommandEntity.builder()
                .aiAgentId("1001")
                .taskType("resume_evaluation")
                .subType("evaluation")
                .sessionId(actualSessionId)
                .maxStep(maxStep == null ? 3 : maxStep)
                .qaFilterExpression(ResumeMetadataSupport.buildKnowledgeFilterExpression(knowledgeSpaceId))
                .resumeId(resumeId)
                .knowledgeSpaceId(knowledgeSpaceId)
                .resumeEvaluationTaskId(evaluationTaskId)
                .message(ResumeWorkflowPromptBuilder.buildEvaluationMessage(resumeId, knowledgeSpaceId, question)
                        + "\n\nJob standard reference:\n" + jobStandardContext)
                .build();
    }

    @Override
    public ResumeEvaluationTaskEntity queryActiveEvaluationTask() {
        return resumeWorkflowRepository.queryActiveEvaluationTask();
    }

    @Override
    public List<ResumeEvaluationTaskEntity> queryRecentEvaluationTasks(Integer limit) {
        return resumeWorkflowRepository.queryRecentEvaluationTasks(normalizeLimit(limit, 20, 50));
    }

    @Override
    public void persistResumeEvaluationResult(ExecuteCommandEntity executeCommandEntity,
                                              DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                              String status,
                                              String errorMessage) {
        if (executeCommandEntity.getResumeEvaluationTaskId() == null) {
            return;
        }
        String report = dynamicContext == null ? null : dynamicContext.getValue("finalSummary");
        if (report == null || report.isBlank()) {
            report = dynamicContext == null ? null : dynamicContext.getValue("executionResult");
        }
        String traceId = dynamicContext == null ? null : dynamicContext.getValue("traceId");
        resumeWorkflowRepository.updateResumeEvaluationTask(
                executeCommandEntity.getResumeEvaluationTaskId(),
                status,
                report,
                traceId,
                errorMessage);
    }

    @Override
    public ResumeInterviewStartEntity startInterview(Long resumeId, Long knowledgeSpaceId, Integer totalRounds) {
        int actualTotalRounds = normalizeInterviewTotalRounds(totalRounds);
        String resumeText = resumeWorkflowRepository.queryResumeText(resumeId);
        String firstQuestion = generateOpeningQuestion(resumeId, knowledgeSpaceId, resumeText);
        String sessionCode = "interview-" + UUID.randomUUID();
        String actualOpeningQuestions = firstQuestion;
        Long interviewSessionId = resumeWorkflowRepository.insertInterviewSession(
                resumeId, knowledgeSpaceId, sessionCode, actualOpeningQuestions, 1, actualTotalRounds, "STARTED");
        resumeWorkflowRepository.insertInterviewRound(interviewSessionId, 1, firstQuestion, "ASKED");

        return ResumeInterviewStartEntity.builder()
                .interviewSessionId(interviewSessionId)
                .currentRound(1)
                .totalRounds(actualTotalRounds)
                .status("STARTED")
                .openingQuestions(firstQuestion)
                .build();
    }

    @Override
    public ExecuteCommandEntity buildInterviewAnswerCommand(Long interviewSessionId, Integer roundNo, String answer, String sessionId, Integer maxStep) {
        Map<String, Object> sessionRow = resumeWorkflowRepository.queryInterviewSessionForAnswer(interviewSessionId);
        String status = (String) sessionRow.get("status");
        if ("FINISHED".equalsIgnoreCase(status)) {
            throw new IllegalStateException("interview already finished, interviewSessionId=" + interviewSessionId);
        }
        Integer currentRound = ((Number) sessionRow.get("current_round")).intValue();
        Integer actualRound = roundNo == null ? currentRound : roundNo;
        if (!actualRound.equals(currentRound)) {
            throw new IllegalStateException("interview round mismatch, interviewSessionId=" + interviewSessionId);
        }
        Integer totalRounds = sessionRow.get("total_rounds") == null
                ? DEFAULT_INTERVIEW_TOTAL_ROUNDS
                : ((Number) sessionRow.get("total_rounds")).intValue();
        Long knowledgeSpaceId = ((Number) sessionRow.get("knowledge_space_id")).longValue();
        List<String> questionRows = resumeWorkflowRepository.queryInterviewRoundQuestions(interviewSessionId, actualRound);
        if (questionRows.isEmpty()) {
            throw new IllegalStateException("interview round not found, interviewSessionId=" + interviewSessionId);
        }
        String currentQuestion = questionRows.get(0);
        resumeWorkflowRepository.updateInterviewRoundAnswer(interviewSessionId, actualRound, answer, "ANSWERED");
        resumeWorkflowRepository.updateInterviewSessionStatus(interviewSessionId, "IN_PROGRESS");

        return ExecuteCommandEntity.builder()
                .aiAgentId("1002")
                .taskType("resume_interview")
                .subType("round_answer")
                .sessionId(sessionId == null || sessionId.isBlank() ? "resume-interview-" + interviewSessionId : sessionId)
                .maxStep(maxStep == null ? 3 : maxStep)
                .qaFilterExpression(ResumeMetadataSupport.buildKnowledgeFilterExpression(knowledgeSpaceId))
                .knowledgeSpaceId(knowledgeSpaceId)
                .interviewSessionId(interviewSessionId)
                .interviewRoundNo(actualRound)
                .interviewTotalRounds(totalRounds)
                .message(ResumeWorkflowPromptBuilder.buildInterviewAnswerMessage(
                        interviewSessionId, actualRound, totalRounds, currentQuestion, answer))
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
                ? DEFAULT_INTERVIEW_TOTAL_ROUNDS
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

        resumeWorkflowRepository.updateInterviewRoundEvaluation(
                interviewSessionId,
                roundNo,
                finalSummary,
                finalRound ? null : truncate(nextQuestion, 500),
                score,
                "EVALUATED");
        if (finalRound) {
            resumeWorkflowRepository.finishInterviewSession(
                    interviewSessionId,
                    roundNo,
                    finalReport == null || finalReport.isBlank() ? finalSummary : finalReport);
            return;
        }
        Integer nextRound = roundNo + 1;
        String actualNextQuestion = (nextQuestion == null || nextQuestion.isBlank())
                ? "请继续基于上一轮回答中的薄弱点，展开更深入的技术追问。"
                : nextQuestion;
        if (!resumeWorkflowRepository.existsInterviewRound(interviewSessionId, nextRound)) {
            resumeWorkflowRepository.insertInterviewRound(interviewSessionId, nextRound, actualNextQuestion, "ASKED");
        } else {
            resumeWorkflowRepository.updateInterviewRoundQuestion(interviewSessionId, nextRound, actualNextQuestion, "ASKED");
        }
        resumeWorkflowRepository.updateInterviewSessionProgress(interviewSessionId, nextRound, "IN_PROGRESS");
    }

    @Override
    public ResumeInterviewDetailEntity queryInterviewDetail(Long interviewSessionId) {
        Map<String, Object> sessionRow = resumeWorkflowRepository.queryInterviewDetailSession(interviewSessionId);
        List<Map<String, Object>> roundRows = resumeWorkflowRepository.queryInterviewDetailRounds(interviewSessionId);
        int totalRounds = sessionRow.get("total_rounds") == null
                ? DEFAULT_INTERVIEW_TOTAL_ROUNDS
                : ((Number) sessionRow.get("total_rounds")).intValue();
        List<ResumeInterviewRoundEntity> rounds = new ArrayList<>();
        for (Map<String, Object> row : roundRows) {
            int actualRoundNo = ((Number) row.get("round_no")).intValue();
            String feedbackContent = (String) row.get("feedback_content");
            ResumeInterviewStructuredResult parsed = ResumeInterviewStructuredResultParser.parse(feedbackContent, actualRoundNo >= totalRounds);
            rounds.add(ResumeInterviewRoundEntity.builder()
                    .roundNo(actualRoundNo)
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

    @Override
    public ResumeInterviewDetailEntity queryActiveInterviewDetail() {
        Long interviewSessionId = resumeWorkflowRepository.queryActiveInterviewSessionId();
        return interviewSessionId == null ? null : queryInterviewDetail(interviewSessionId);
    }

    private String readResumeText(byte[] bytes, String fileName) {
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        StringBuilder content = new StringBuilder();
        for (Document document : new TikaDocumentReader(resource).get()) {
            if (document.getText() != null) {
                content.append(document.getText()).append("\n");
            }
        }
        return content.toString();
    }

    private String generateOpeningQuestion(Long resumeId, Long knowledgeSpaceId, String resumeText) {
        String fallback = "请结合你简历中最有代表性的项目，介绍项目目标、你的职责，以及你解决过的一个关键技术问题。";
        if (applicationContext == null) {
            return fallback;
        }
        try {
            String interviewContext = retrieveInterviewOpeningContext(knowledgeSpaceId, resumeText);
            ChatClient chatClient = (ChatClient) applicationContext.getBean(AiAgentEnumVO.AI_CLIENT.getBeanName("5201"));
            ChatResponse response = chatClient.prompt(ResumeWorkflowPromptBuilder.buildInterviewOpeningPrompt(
                            resumeId,
                            knowledgeSpaceId,
                            interviewContext))
                    .call()
                    .chatResponse();
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                return fallback;
            }
            String text = response.getResult().getOutput().getText();
            String normalized = normalizeOpeningQuestion(text);
            return normalized == null || normalized.isBlank() ? fallback : normalized;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String retrieveInterviewOpeningContext(Long knowledgeSpaceId, String resumeText) {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query("候选人的项目经历、核心职责、技术栈、架构设计、性能优化、排障经验、业务结果")
                .topK(4)
                .similarityThreshold(0.1d)
                .filterExpression(ResumeMetadataSupport.buildKnowledgeFilterExpression(knowledgeSpaceId))
                .build());
        if (documents == null || documents.isEmpty()) {
            return truncate(resumeText, 1800);
        }
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            context.append("[片段").append(i + 1).append("]\n")
                    .append(truncate(document.getText(), 600))
                    .append("\n");
        }
        return truncate(context.toString(), 2400);
    }

    private String normalizeOpeningQuestion(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        List<String> questions = parseQuestionList(text.replace("\r\n", "\n").trim());
        if (!questions.isEmpty()) {
            return truncate(questions.get(0), 220);
        }
        String normalized = text.trim();
        int lineBreak = normalized.indexOf('\n');
        if (lineBreak > 0) {
            normalized = normalized.substring(0, lineBreak).trim();
        }
        return truncate(normalized, 220);
    }

    private int normalizeInterviewTotalRounds(Integer totalRounds) {
        if (totalRounds == null) {
            return DEFAULT_INTERVIEW_TOTAL_ROUNDS;
        }
        if (totalRounds == 3 || totalRounds == 5 || totalRounds == 8) {
            return totalRounds;
        }
        return DEFAULT_INTERVIEW_TOTAL_ROUNDS;
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
            return "No job standard chunks retrieved. Use general Java backend standards conservatively.";
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
        String compact = firstLine.replaceAll("[*#>`-]", " ").replaceAll("\\s+", " ").trim();
        return compact.isBlank() ? "N/A" : truncate(compact, MAX_SCORE_LENGTH);
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
            String normalized = line.replaceFirst("^[0-9]+[.、]\\s*", "").replaceFirst("^[-*]\\s*", "");
            if (!normalized.isBlank()) {
                questions.add(normalized);
            }
        }
        if (questions.isEmpty()) {
            questions.add(openingQuestions.trim());
        }
        return questions;
    }

    private int normalizeLimit(Integer limit, int defaultValue, int maxValue) {
        if (limit == null || limit <= 0) {
            return defaultValue;
        }
        return Math.min(limit, maxValue);
    }
}
