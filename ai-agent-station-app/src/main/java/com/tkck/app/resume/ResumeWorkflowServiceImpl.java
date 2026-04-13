package com.tkck.app.resume;

import com.tkck.config.AiAgentConfig;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.model.valobj.enums.AiAgentEnumVO;
import com.tkck.domain.resume.model.entity.ResumeInterviewStartEntity;
import com.tkck.domain.resume.model.entity.ResumeUploadResultEntity;
import com.tkck.domain.resume.service.IResumeWorkflowService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ResumeWorkflowServiceImpl implements IResumeWorkflowService {

    @Resource(name = "mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbcTemplate;

    @Resource(name = "vectorStore")
    private VectorStore vectorStore;

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
        return ExecuteCommandEntity.builder()
                .aiAgentId("1001")
                .sessionId(sessionId == null || sessionId.isBlank() ? "resume-eval-" + knowledgeSpaceId : sessionId)
                .maxStep(maxStep == null ? 3 : maxStep)
                .qaFilterExpression(ResumeMetadataSupport.buildKnowledgeFilterExpression(knowledgeSpaceId))
                .message(ResumeWorkflowPromptBuilder.buildEvaluationMessage(resumeId, knowledgeSpaceId, question))
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

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String sessionCode = "interview-" + UUID.randomUUID();
        mysqlJdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO resume_interview_session (resume_id, knowledge_space_id, session_code, opening_questions, current_round, status) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, resumeId);
            ps.setLong(2, knowledgeSpaceId);
            ps.setString(3, sessionCode);
            ps.setString(4, openingQuestions);
            ps.setInt(5, 1);
            ps.setString(6, "STARTED");
            return ps;
        }, keyHolder);
        Long interviewSessionId = keyHolder.getKey().longValue();

        mysqlJdbcTemplate.update(
                "INSERT INTO resume_interview_round (interview_session_id, round_no, question_content, status) VALUES (?, ?, ?, ?)",
                interviewSessionId, 1, openingQuestions, "ASKED");

        return ResumeInterviewStartEntity.builder()
                .interviewSessionId(interviewSessionId)
                .currentRound(1)
                .openingQuestions(openingQuestions)
                .build();
    }

    @Override
    public ExecuteCommandEntity buildInterviewAnswerCommand(Long interviewSessionId, Integer roundNo, String answer, String sessionId, Integer maxStep) {
        Map<String, Object> sessionRow = mysqlJdbcTemplate.queryForMap(
                "SELECT id, knowledge_space_id, opening_questions, current_round FROM resume_interview_session WHERE id = ?",
                interviewSessionId);
        Integer actualRound = roundNo == null ? ((Number) sessionRow.get("current_round")).intValue() : roundNo;
        String currentQuestion = (String) sessionRow.get("opening_questions");
        Long knowledgeSpaceId = ((Number) sessionRow.get("knowledge_space_id")).longValue();

        mysqlJdbcTemplate.update(
                "UPDATE resume_interview_round SET answer_content = ?, status = ?, update_time = NOW() WHERE interview_session_id = ? AND round_no = ?",
                answer, "ANSWERED", interviewSessionId, actualRound);

        return ExecuteCommandEntity.builder()
                .aiAgentId("1002")
                .sessionId(sessionId == null || sessionId.isBlank() ? "resume-interview-" + interviewSessionId : sessionId)
                .maxStep(maxStep == null ? 3 : maxStep)
                .qaFilterExpression(ResumeMetadataSupport.buildKnowledgeFilterExpression(knowledgeSpaceId))
                .message(ResumeWorkflowPromptBuilder.buildInterviewAnswerMessage(interviewSessionId, actualRound, currentQuestion, answer))
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
}
