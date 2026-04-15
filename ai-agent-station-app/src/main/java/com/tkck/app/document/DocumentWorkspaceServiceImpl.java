package com.tkck.app.document;

import com.alibaba.fastjson.JSON;
import com.tkck.config.AiAgentConfig;
import com.tkck.domain.agent.model.valobj.enums.AiAgentEnumVO;
import com.tkck.domain.document.model.entity.DocumentFileEntity;
import com.tkck.domain.document.model.entity.DocumentRetrievedChunkEntity;
import com.tkck.domain.document.model.entity.DocumentTaskResultEntity;
import com.tkck.domain.document.model.entity.DocumentWorkspaceDetailEntity;
import com.tkck.domain.document.model.entity.DocumentWorkspaceEntity;
import com.tkck.domain.document.service.IDocumentWorkspaceService;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentWorkspaceServiceImpl implements IDocumentWorkspaceService {

    private static final int DEFAULT_TOP_K = 6;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.1D;
    private static final int MAX_CONTEXT_LENGTH = 280;

    private final JdbcTemplate mysqlJdbcTemplate;
    private final VectorStore documentVectorStore;
    private final TokenTextSplitter tokenTextSplitter;

    @Resource
    private ApplicationContext applicationContext;

    public DocumentWorkspaceServiceImpl(JdbcTemplate mysqlJdbcTemplate,
                                        VectorStore documentVectorStore,
                                        TokenTextSplitter tokenTextSplitter) {
        this.mysqlJdbcTemplate = mysqlJdbcTemplate;
        this.documentVectorStore = documentVectorStore;
        this.tokenTextSplitter = tokenTextSplitter;
    }

    @Override
    public DocumentWorkspaceEntity createWorkspace(String workspaceName, String description) {
        String workspaceId = "dws_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.info("document workspace create start, workspaceName={}, description={}", workspaceName, description);
        mysqlJdbcTemplate.update(
                "INSERT INTO ai_knowledge_space (space_id, space_name, space_type, description, status) VALUES (?, ?, ?, ?, ?)",
                workspaceId,
                workspaceName,
                "document",
                description,
                1
        );
        DocumentWorkspaceEntity workspace = DocumentWorkspaceEntity.builder()
                .workspaceId(workspaceId)
                .workspaceName(workspaceName)
                .description(description)
                .status("READY")
                .build();
        log.info("document workspace created, workspaceId={}, workspaceName={}", workspaceId, workspaceName);
        return workspace;
    }

    @Override
    public List<DocumentWorkspaceEntity> listWorkspaces() {
        List<Map<String, Object>> rows = mysqlJdbcTemplate.queryForList("""
                SELECT s.space_id,
                       s.space_name,
                       s.description,
                       s.status,
                       s.update_time,
                       COUNT(d.doc_id) AS document_count
                FROM ai_knowledge_space s
                LEFT JOIN ai_knowledge_document d ON s.space_id = d.space_id AND d.status = 1
                WHERE s.space_type = 'document' AND s.status = 1
                GROUP BY s.space_id, s.space_name, s.description, s.status, s.update_time
                ORDER BY s.update_time DESC, s.id DESC
                """);
        return rows.stream()
                .map(this::toWorkspaceEntity)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentFileEntity upload(String workspaceId, MultipartFile file) throws Exception {
        validateWorkspace(workspaceId);
        validateFile(file);

        String fileName = file.getOriginalFilename() == null ? "document.txt" : file.getOriginalFilename();
        String fileType = detectFileType(fileName);
        String docId = "doc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        byte[] bytes = file.getBytes();
        log.info("document upload start, workspaceId={}, docId={}, fileName={}, fileType={}, fileSize={}",
                workspaceId, docId, fileName, fileType, file.getSize());
        String rawText = readDocumentText(bytes, fileName);
        log.info("document parse completed, workspaceId={}, docId={}, textLength={}", workspaceId, docId, rawText.length());

        mysqlJdbcTemplate.update(
                "INSERT INTO ai_knowledge_document (doc_id, space_id, file_name, file_type, file_size, parse_status, chunk_count, vector_status, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                docId,
                workspaceId,
                fileName,
                fileType,
                file.getSize(),
                "PARSING",
                0,
                "PENDING",
                1
        );

        List<Document> splitDocuments = tokenTextSplitter.apply(List.of(new Document(rawText)));
        log.info("document split completed, workspaceId={}, docId={}, chunkCount={}", workspaceId, docId, splitDocuments.size());
        for (int i = 0; i < splitDocuments.size(); i++) {
            Document document = splitDocuments.get(i);
            Map<String, Object> metadata = DocumentWorkspaceMetadataSupport.buildChunkMetadata(workspaceId, docId, fileName, i);
            metadata.forEach(document.getMetadata()::put);
            mysqlJdbcTemplate.update(
                    "INSERT INTO ai_knowledge_chunk (chunk_id, doc_id, space_id, chunk_index, chunk_text, metadata_json) VALUES (?, ?, ?, ?, ?, ?)",
                    "chunk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                    docId,
                    workspaceId,
                    i,
                    document.getText(),
                    JSON.toJSONString(metadata)
            );
        }

        log.info("document vector store write start, workspaceId={}, docId={}, chunkCount={}", workspaceId, docId, splitDocuments.size());
        documentVectorStore.accept(splitDocuments);

        mysqlJdbcTemplate.update(
                "UPDATE ai_knowledge_document SET parse_status = ?, chunk_count = ?, vector_status = ?, update_time = NOW() WHERE doc_id = ?",
                "COMPLETED",
                splitDocuments.size(),
                "COMPLETED",
                docId
        );

        DocumentFileEntity result = DocumentFileEntity.builder()
                .docId(docId)
                .workspaceId(workspaceId)
                .fileName(fileName)
                .fileType(fileType)
                .fileSize(file.getSize())
                .parseStatus("COMPLETED")
                .chunkCount(splitDocuments.size())
                .vectorStatus("COMPLETED")
                .build();
        log.info("document upload completed, workspaceId={}, docId={}, chunkCount={}, vectorStatus={}",
                workspaceId, docId, splitDocuments.size(), result.getVectorStatus());
        return result;
    }

    @Override
    public DocumentWorkspaceDetailEntity queryWorkspaceDetail(String workspaceId) {
        Map<String, Object> workspaceRow = mysqlJdbcTemplate.queryForMap(
                "SELECT space_id, space_name, description, status FROM ai_knowledge_space WHERE space_id = ?",
                workspaceId
        );
        List<Map<String, Object>> documentRows = mysqlJdbcTemplate.queryForList(
                "SELECT doc_id, file_name, file_type, file_size, parse_status, chunk_count, vector_status FROM ai_knowledge_document WHERE space_id = ? ORDER BY create_time DESC",
                workspaceId
        );
        List<DocumentFileEntity> documents = new ArrayList<>();
        for (Map<String, Object> row : documentRows) {
            documents.add(DocumentFileEntity.builder()
                    .docId((String) row.get("doc_id"))
                    .workspaceId(workspaceId)
                    .fileName((String) row.get("file_name"))
                    .fileType((String) row.get("file_type"))
                    .fileSize(row.get("file_size") == null ? 0L : ((Number) row.get("file_size")).longValue())
                    .parseStatus((String) row.get("parse_status"))
                    .chunkCount(row.get("chunk_count") == null ? 0 : ((Number) row.get("chunk_count")).intValue())
                    .vectorStatus((String) row.get("vector_status"))
                    .build());
        }
        return DocumentWorkspaceDetailEntity.builder()
                .workspaceId((String) workspaceRow.get("space_id"))
                .workspaceName((String) workspaceRow.get("space_name"))
                .description((String) workspaceRow.get("description"))
                .status(String.valueOf(workspaceRow.get("status")))
                .documentCount(documents.size())
                .documents(documents)
                .build();
    }

    @Override
    public DocumentTaskResultEntity ask(String workspaceId, String docId, String question) {
        return executeDocumentTask(
                workspaceId,
                docId,
                question,
                DocumentWorkspacePromptBuilder.buildAskPrompt(workspaceId, docId, question)
        );
    }

    @Override
    public DocumentTaskResultEntity summary(String workspaceId, String docId, String summaryMode) {
        String actualMode = StringUtils.hasText(summaryMode) ? summaryMode.trim() : "结构化摘要";
        return executeDocumentTask(
                workspaceId,
                docId,
                "请基于当前文档空间生成" + actualMode,
                DocumentWorkspacePromptBuilder.buildSummaryPrompt(workspaceId, docId, actualMode)
        );
    }

    @Override
    public DocumentTaskResultEntity followup(String workspaceId, String docId, String perspective) {
        String actualPerspective = StringUtils.hasText(perspective) ? perspective.trim() : "通用";
        return executeDocumentTask(
                workspaceId,
                docId,
                "请从" + actualPerspective + "视角生成文档追问",
                DocumentWorkspacePromptBuilder.buildFollowupPrompt(workspaceId, docId, actualPerspective)
        );
    }

    @Override
    public DocumentTaskResultEntity quiz(String workspaceId, String docId, Integer questionCount, String quizType) {
        int actualCount = questionCount == null || questionCount <= 0 ? 5 : questionCount;
        String actualQuizType = StringUtils.hasText(quizType) ? quizType.trim() : "简答题";
        return executeDocumentTask(
                workspaceId,
                docId,
                "请基于当前文档生成" + actualCount + "道" + actualQuizType,
                DocumentWorkspacePromptBuilder.buildQuizPrompt(workspaceId, docId, actualCount, actualQuizType)
        );
    }

    protected String readDocumentText(byte[] bytes, String fileName) {
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

    protected String generateAnswer(String prompt, List<Document> documents) {
        String finalContext = buildFinalContext(documents);
        if (applicationContext == null) {
            return prompt + "\n\n检索上下文:\n" + finalContext;
        }

        ChatClient chatClient = getChatClient("5201");
        return chatClient.prompt(prompt + "\n\n检索到的文档上下文:\n" + finalContext)
                .call()
                .content();
    }

    private DocumentTaskResultEntity executeDocumentTask(String workspaceId,
                                                         String docId,
                                                         String query,
                                                         String prompt) {
        String taskType = resolveTaskType(prompt);
        String retrievalScope = buildRetrievalScope(workspaceId, docId);
        log.info("document task start, taskType={}, workspaceId={}, docId={}, retrievalScope={}",
                taskType, workspaceId, docId == null ? "ALL" : docId, retrievalScope);
        try {
            validateWorkspace(workspaceId);
            validateDocument(workspaceId, docId);

            String rewrittenQuery = rewriteQuery(query);
            log.info("document query rewritten, taskType={}, workspaceId={}, docId={}, rewrittenQuery={}",
                    taskType, workspaceId, docId == null ? "ALL" : docId, rewrittenQuery);
            List<Document> retrievedDocuments = retrieveDocuments(workspaceId, docId, rewrittenQuery);
            String finalContext = buildFinalContext(retrievedDocuments);
            log.info("document retrieval completed, taskType={}, workspaceId={}, docId={}, retrievedChunks={}",
                    taskType, workspaceId, docId == null ? "ALL" : docId, retrievedDocuments == null ? 0 : retrievedDocuments.size());

            DocumentTaskResultEntity result = DocumentTaskResultEntity.builder()
                    .answer(generateAnswer(prompt, retrievedDocuments))
                    .rewrittenQuery(rewrittenQuery)
                    .retrievalScope(retrievalScope)
                    .finalContext(finalContext)
                    .retrievedChunks(toChunkTexts(retrievedDocuments))
                    .retrievedChunkDetails(toRetrievedChunkDetails(retrievedDocuments))
                    .build();
            log.info("document task completed, taskType={}, workspaceId={}, docId={}, answerLength={}",
                    taskType, workspaceId, docId == null ? "ALL" : docId, result.getAnswer() == null ? 0 : result.getAnswer().length());
            return result;
        } catch (Exception e) {
            log.error("document task failed, taskType={}, workspaceId={}, docId={}, message={}",
                    taskType, workspaceId, docId == null ? "ALL" : docId, e.getMessage(), e);
            throw e;
        }
    }

    private void validateWorkspace(String workspaceId) {
        Integer count = mysqlJdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ai_knowledge_space WHERE space_id = ? AND space_type = 'document'",
                Integer.class,
                workspaceId
        );
        if (count == null || count == 0) {
            throw new IllegalArgumentException("document workspace not found: " + workspaceId);
        }
    }

    private void validateDocument(String workspaceId, String docId) {
        if (!StringUtils.hasText(docId)) {
            return;
        }
        Integer count = mysqlJdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ai_knowledge_document WHERE space_id = ? AND doc_id = ? AND status = 1",
                Integer.class,
                workspaceId,
                docId
        );
        if (count == null || count == 0) {
            throw new IllegalArgumentException("document not found in workspace: " + docId);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("document file is empty");
        }
        detectFileType(file.getOriginalFilename());
    }

    private String detectFileType(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            throw new IllegalArgumentException("unsupported file type");
        }
        String suffix = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        if (!List.of("pdf", "docx", "txt", "md", "markdown").contains(suffix)) {
            throw new IllegalArgumentException("unsupported file type: " + suffix);
        }
        if ("markdown".equals(suffix)) {
            return "md";
        }
        return suffix;
    }

    private List<Document> retrieveDocuments(String workspaceId, String docId, String query) {
        List<Document> retrievedDocuments = documentVectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(DEFAULT_TOP_K)
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                .filterExpression(buildFilterExpression(workspaceId, docId))
                .build());
        return filterRetrievedDocuments(workspaceId, docId, retrievedDocuments, loadAllowedDocumentIds(workspaceId, docId));
    }

    private List<Document> filterRetrievedDocuments(String workspaceId,
                                                    String docId,
                                                    List<Document> documents,
                                                    Set<String> allowedDocumentIds) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream()
                .filter(document -> workspaceId.equals(String.valueOf(document.getMetadata().get("spaceId"))))
                .filter(document -> !StringUtils.hasText(docId) || docId.equals(String.valueOf(document.getMetadata().get("docId"))))
                .filter(document -> allowedDocumentIds.contains(String.valueOf(document.getMetadata().get("docId"))))
                .toList();
    }

    private Set<String> loadAllowedDocumentIds(String workspaceId, String docId) {
        if (StringUtils.hasText(docId)) {
            return Set.of(docId);
        }
        List<Map<String, Object>> rows = mysqlJdbcTemplate.queryForList(
                "SELECT doc_id FROM ai_knowledge_document WHERE space_id = ? AND status = 1",
                workspaceId
        );
        Set<String> allowedDocumentIds = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Object value = row.get("doc_id");
            if (value != null) {
                allowedDocumentIds.add(String.valueOf(value));
            }
        }
        return allowedDocumentIds;
    }

    private String buildFilterExpression(String workspaceId, String docId) {
        return StringUtils.hasText(docId)
                ? DocumentWorkspaceMetadataSupport.buildDocumentFilterExpression(workspaceId, docId)
                : DocumentWorkspaceMetadataSupport.buildWorkspaceFilterExpression(workspaceId);
    }

    private String rewriteQuery(String question) {
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException("document question is empty");
        }
        return question.trim();
    }

    private String buildFinalContext(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "未检索到可用文档片段，请基于当前 Workspace 的文档重新提问。";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (Document document : documents) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(index++)
                    .append(". ")
                    .append(truncate(document.getText(), MAX_CONTEXT_LENGTH));
        }
        return builder.toString();
    }

    private List<String> toChunkTexts(List<Document> documents) {
        List<String> chunks = new ArrayList<>();
        if (documents == null) {
            return chunks;
        }
        for (Document document : documents) {
            chunks.add(document.getText());
        }
        return chunks;
    }

    private List<DocumentRetrievedChunkEntity> toRetrievedChunkDetails(List<Document> documents) {
        List<DocumentRetrievedChunkEntity> chunks = new ArrayList<>();
        if (documents == null) {
            return chunks;
        }
        for (Document document : documents) {
            chunks.add(DocumentRetrievedChunkEntity.builder()
                    .workspaceId(stringMetadata(document, "spaceId"))
                    .docId(stringMetadata(document, "docId"))
                    .fileName(stringMetadata(document, "fileName"))
                    .chunkIndex(stringMetadata(document, "chunkIndex"))
                    .preview(truncate(document.getText(), MAX_CONTEXT_LENGTH))
                    .build());
        }
        return chunks;
    }

    private String buildRetrievalScope(String workspaceId, String docId) {
        return docId == null || docId.isBlank()
                ? "workspace:" + workspaceId + ",vectorTable=" + AiAgentConfig.DOCUMENT_VECTOR_TABLE
                : "workspace:" + workspaceId + ",doc:" + docId + ",vectorTable=" + AiAgentConfig.DOCUMENT_VECTOR_TABLE;
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

    private String resolveTaskType(String prompt) {
        if (prompt == null) {
            return "ask";
        }
        if (prompt.contains("摘要")) {
            return "summary";
        }
        if (prompt.contains("追问")) {
            return "followup";
        }
        if (prompt.contains("测验")) {
            return "quiz";
        }
        return "ask";
    }

    private String stringMetadata(Document document, String key) {
        Object value = document.getMetadata().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private DocumentWorkspaceEntity toWorkspaceEntity(Map<String, Object> row) {
        return DocumentWorkspaceEntity.builder()
                .workspaceId((String) row.get("space_id"))
                .workspaceName((String) row.get("space_name"))
                .description((String) row.get("description"))
                .status(String.valueOf(row.get("status")))
                .documentCount(row.get("document_count") == null ? 0 : ((Number) row.get("document_count")).intValue())
                .updateTime(row.get("update_time") == null ? null : String.valueOf(row.get("update_time")))
                .build();
    }
}
