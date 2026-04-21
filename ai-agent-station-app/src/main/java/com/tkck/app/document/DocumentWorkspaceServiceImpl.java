package com.tkck.app.document;

import com.alibaba.fastjson.JSON;
import com.tkck.config.AiAgentConfig;
import com.tkck.domain.agent.model.valobj.enums.AiAgentEnumVO;
import com.tkck.domain.audit.model.entity.AuditEventEntity;
import com.tkck.domain.audit.model.entity.AuditExecutionMetricEntity;
import com.tkck.domain.audit.model.entity.AuditLlmCallMetricEntity;
import com.tkck.domain.audit.model.entity.AuditStepMetricEntity;
import com.tkck.domain.audit.service.IAuditMonitoringService;
import com.tkck.domain.document.adapter.repository.IDocumentWorkspaceRepository;
import com.tkck.domain.document.model.entity.DocumentFileEntity;
import com.tkck.domain.document.model.entity.DocumentQueryRewriteCommandEntity;
import com.tkck.domain.document.model.entity.DocumentQueryRewriteResultEntity;
import com.tkck.domain.document.model.entity.DocumentRetrievedChunkEntity;
import com.tkck.domain.document.model.entity.DocumentTaskRecordEntity;
import com.tkck.domain.document.model.entity.DocumentTaskResultEntity;
import com.tkck.domain.document.model.entity.DocumentWorkspaceDetailEntity;
import com.tkck.domain.document.model.entity.DocumentWorkspaceEntity;
import com.tkck.domain.document.service.IDocumentWorkspaceService;
import com.tkck.domain.document.service.IQueryRewriteService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentWorkspaceServiceImpl implements IDocumentWorkspaceService {

    static final String DOCUMENT_CHAT_CLIENT_ID = "5401";
    static final String DOCUMENT_CHAT_MODEL_CODE = "2008";
    private static final int DEFAULT_TOP_K = 6;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.1D;
    private static final int MAX_CONTEXT_LENGTH = 280;

    private final IDocumentWorkspaceRepository documentWorkspaceRepository;
    private final VectorStore documentVectorStore;
    private final TokenTextSplitter tokenTextSplitter;
    private final IQueryRewriteService queryRewriteService;

    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private IAuditMonitoringService auditMonitoringService;

    public DocumentWorkspaceServiceImpl(IDocumentWorkspaceRepository documentWorkspaceRepository,
                                        @Qualifier("documentVectorStore") VectorStore documentVectorStore,
                                        TokenTextSplitter tokenTextSplitter,
                                        IQueryRewriteService queryRewriteService) {
        this.documentWorkspaceRepository = documentWorkspaceRepository;
        this.documentVectorStore = documentVectorStore;
        this.tokenTextSplitter = tokenTextSplitter;
        this.queryRewriteService = queryRewriteService;
    }

    @Override
    public DocumentWorkspaceEntity createWorkspace(String workspaceName, String description) {
        String workspaceId = "dws_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.info("document workspace create start, workspaceName={}, description={}", workspaceName, description);
        documentWorkspaceRepository.saveWorkspace(workspaceId, workspaceName, description);
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
        return documentWorkspaceRepository.queryWorkspaceList();
    }

    @Override
    public DocumentWorkspaceDetailEntity queryActiveWorkspace() {
        List<DocumentWorkspaceEntity> workspaces = listWorkspaces();
        if (workspaces.isEmpty()) {
            return null;
        }
        return queryWorkspaceDetail(workspaces.get(0).getWorkspaceId());
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

        documentWorkspaceRepository.saveDocument(DocumentFileEntity.builder()
                .docId(docId)
                .workspaceId(workspaceId)
                .fileName(fileName)
                .fileType(fileType)
                .fileSize(file.getSize())
                .parseStatus("PARSING")
                .chunkCount(0)
                .vectorStatus("PENDING")
                .build());

        List<Document> splitDocuments = tokenTextSplitter.apply(List.of(new Document(rawText)));
        log.info("document split completed, workspaceId={}, docId={}, chunkCount={}", workspaceId, docId, splitDocuments.size());
        for (int i = 0; i < splitDocuments.size(); i++) {
            Document document = splitDocuments.get(i);
            Map<String, Object> metadata = DocumentWorkspaceMetadataSupport.buildChunkMetadata(workspaceId, docId, fileName, i);
            metadata.forEach(document.getMetadata()::put);
            documentWorkspaceRepository.saveChunk(
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

        documentWorkspaceRepository.updateDocumentParseResult(docId, "COMPLETED", splitDocuments.size(), "COMPLETED");

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
        return documentWorkspaceRepository.queryWorkspaceDetail(workspaceId);
    }

    @Override
    public DocumentTaskResultEntity ask(String workspaceId, String docId, String question) {
        return executeDocumentTask(
                workspaceId,
                docId,
                "ask",
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
                "summary",
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
                "followup",
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
                "quiz",
                "请基于当前文档生成" + actualCount + "道" + actualQuizType,
                DocumentWorkspacePromptBuilder.buildQuizPrompt(workspaceId, docId, actualCount, actualQuizType)
        );
    }

    @Override
    public List<DocumentTaskRecordEntity> queryRecentTasks(String workspaceId, Integer limit) {
        int actualLimit = normalizeLimit(limit, 10, 50);
        return documentWorkspaceRepository.queryRecentTasks(workspaceId, actualLimit);
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

    protected String generateAnswer(String traceId,
                                    String taskType,
                                    String workspaceId,
                                    String sessionId,
                                    String prompt,
                                    List<Document> documents) {
        String finalContext = buildFinalContext(documents);
        if (applicationContext == null) {
            return prompt + "\n\n检索上下文:\n" + finalContext;
        }

        long startTime = System.currentTimeMillis();
        ChatClient chatClient = getChatClient(DOCUMENT_CHAT_CLIENT_ID);
        ChatResponse response = chatClient.prompt(prompt + "\n\n检索到的文档上下文:\n" + finalContext)
                .call()
                .chatResponse();
        String content = response == null || response.getResult() == null || response.getResult().getOutput() == null
                ? ""
                : safe(response.getResult().getOutput().getText());
        Usage usage = response == null || response.getMetadata() == null ? null : response.getMetadata().getUsage();
        if (auditMonitoringService != null) {
            auditMonitoringService.recordLlmCall(AuditLlmCallMetricEntity.builder()
                    .traceId(traceId)
                    .taskType("document_workspace")
                    .taskSubType(taskType)
                    .taskId(workspaceId)
                    .sessionId(sessionId)
                    .stepName(taskType)
                    .stage("DOCUMENT_" + taskType.toUpperCase())
                    .clientId(DOCUMENT_CHAT_CLIENT_ID)
                    .modelCode(DOCUMENT_CHAT_MODEL_CODE)
                    .status("SUCCESS")
                    .durationMs(System.currentTimeMillis() - startTime)
                    .promptTokens(usage == null ? 0L : usage.getPromptTokens())
                    .completionTokens(usage == null ? 0L : usage.getCompletionTokens())
                    .totalTokens(usage == null ? 0L : usage.getTotalTokens())
                    .location("DocumentWorkspaceServiceImpl#generateAnswer")
                    .build());
        }
        return content;
    }

    private DocumentTaskResultEntity executeDocumentTask(String workspaceId,
                                                         String docId,
                                                         String taskType,
                                                         String query,
                                                         String prompt) {
        String traceId = "trace_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String sessionId = "document-" + workspaceId + "-" + taskType + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String retrievalScope = buildRetrievalScope(workspaceId, docId);
        log.info("document task start, taskType={}, workspaceId={}, docId={}, retrievalScope={}",
                taskType, workspaceId, docId == null ? "ALL" : docId, retrievalScope);
        if (auditMonitoringService != null) {
            auditMonitoringService.startExecution(AuditExecutionMetricEntity.builder()
                    .traceId(traceId)
                    .taskType("document_workspace")
                    .taskSubType(taskType)
                    .taskId(workspaceId)
                    .sessionId(sessionId)
                    .executionMode("SINGLE_SHOT")
                    .status("RUNNING")
                    .build());
            auditMonitoringService.recordEvent(AuditEventEntity.builder()
                    .eventType("DOCUMENT_TASK_START")
                    .bizType("document_workspace")
                    .bizId(workspaceId)
                    .sessionId(sessionId)
                    .executionMode("SINGLE_SHOT")
                    .status("RUNNING")
                    .location("DocumentWorkspaceServiceImpl#executeDocumentTask")
                    .metadataJson(JSON.toJSONString(Map.of("traceId", traceId, "taskType", taskType, "docId", docId == null ? "ALL" : docId)))
                    .build());
        }
        long startTime = System.currentTimeMillis();
        try {
            validateWorkspace(workspaceId);
            validateDocument(workspaceId, docId);

            String rewrittenQuery = resolveRewrittenQuery(taskType, workspaceId, docId, query);
            log.info("document query rewritten, taskType={}, workspaceId={}, docId={}, rewrittenQuery={}",
                    taskType, workspaceId, docId == null ? "ALL" : docId, rewrittenQuery);
            List<Document> retrievedDocuments = retrieveDocuments(workspaceId, docId, rewrittenQuery);
            String finalContext = buildFinalContext(retrievedDocuments);
            log.info("document retrieval completed, taskType={}, workspaceId={}, docId={}, retrievedChunks={}",
                    taskType, workspaceId, docId == null ? "ALL" : docId, retrievedDocuments == null ? 0 : retrievedDocuments.size());

            DocumentTaskResultEntity result = DocumentTaskResultEntity.builder()
                    .answer(generateAnswer(traceId, taskType, workspaceId, sessionId, prompt, retrievedDocuments))
                    .rewrittenQuery(rewrittenQuery)
                    .retrievalScope(retrievalScope)
                    .finalContext(finalContext)
                    .retrievedChunks(toChunkTexts(retrievedDocuments))
                    .retrievedChunkDetails(toRetrievedChunkDetails(retrievedDocuments))
                    .build();
            persistDocumentTaskRecord(workspaceId, docId, taskType, query, result, "SUCCESS", null);
            log.info("document task completed, taskType={}, workspaceId={}, docId={}, answerLength={}",
                    taskType, workspaceId, docId == null ? "ALL" : docId, result.getAnswer() == null ? 0 : result.getAnswer().length());
            if (auditMonitoringService != null) {
                auditMonitoringService.recordStep(AuditStepMetricEntity.builder()
                        .traceId(traceId)
                        .taskId(workspaceId)
                        .sessionId(sessionId)
                        .stepNo(1)
                        .stepName(taskType)
                        .stage("DOCUMENT_" + taskType.toUpperCase())
                        .clientId(DOCUMENT_CHAT_CLIENT_ID)
                        .modelCode(DOCUMENT_CHAT_MODEL_CODE)
                        .status("SUCCESS")
                        .durationMs(System.currentTimeMillis() - startTime)
                        .retryCount(0)
                        .timeoutFlag(false)
                        .degradedFlag(false)
                        .location("DocumentWorkspaceServiceImpl#executeDocumentTask")
                        .build());
                auditMonitoringService.finishExecution(traceId, "SUCCESS", System.currentTimeMillis() - startTime);
                auditMonitoringService.recordEvent(AuditEventEntity.builder()
                        .eventType("DOCUMENT_TASK_COMPLETE")
                        .bizType("document_workspace")
                        .bizId(workspaceId)
                        .sessionId(sessionId)
                        .executionMode("SINGLE_SHOT")
                        .status("SUCCESS")
                        .location("DocumentWorkspaceServiceImpl#executeDocumentTask")
                        .metadataJson(JSON.toJSONString(Map.of("traceId", traceId, "taskType", taskType, "docId", docId == null ? "ALL" : docId)))
                        .build());
            }
            return result;
        } catch (Exception e) {
            persistDocumentTaskRecord(workspaceId, docId, taskType, query, null, "FAILED", e.getMessage());
            log.error("document task failed, taskType={}, workspaceId={}, docId={}, message={}",
                    taskType, workspaceId, docId == null ? "ALL" : docId, e.getMessage(), e);
            if (auditMonitoringService != null) {
                auditMonitoringService.recordStep(AuditStepMetricEntity.builder()
                        .traceId(traceId)
                        .taskId(workspaceId)
                        .sessionId(sessionId)
                        .stepNo(1)
                        .stepName(taskType)
                        .stage("DOCUMENT_" + taskType.toUpperCase())
                        .clientId(DOCUMENT_CHAT_CLIENT_ID)
                        .modelCode(DOCUMENT_CHAT_MODEL_CODE)
                        .status("FAILED")
                        .durationMs(System.currentTimeMillis() - startTime)
                        .retryCount(0)
                        .timeoutFlag(false)
                        .degradedFlag(false)
                        .errorCode(e.getClass().getSimpleName())
                        .errorMessage(e.getMessage())
                        .location("DocumentWorkspaceServiceImpl#executeDocumentTask")
                        .build());
                auditMonitoringService.finishExecution(traceId, "FAILED", System.currentTimeMillis() - startTime);
                auditMonitoringService.recordEvent(AuditEventEntity.builder()
                        .eventType("DOCUMENT_TASK_FAILED")
                        .bizType("document_workspace")
                        .bizId(workspaceId)
                        .sessionId(sessionId)
                        .executionMode("SINGLE_SHOT")
                        .status("FAILED")
                        .errorCode(e.getClass().getSimpleName())
                        .errorMessage(e.getMessage())
                        .location("DocumentWorkspaceServiceImpl#executeDocumentTask")
                        .metadataJson(JSON.toJSONString(Map.of("traceId", traceId, "taskType", taskType, "docId", docId == null ? "ALL" : docId)))
                        .build());
            }
            throw e;
        }
    }

    private void persistDocumentTaskRecord(String workspaceId,
                                           String docId,
                                           String mode,
                                           String question,
                                           DocumentTaskResultEntity result,
                                           String status,
                                           String errorMessage) {
        documentWorkspaceRepository.saveTaskRecord(DocumentTaskRecordEntity.builder()
                .workspaceId(workspaceId)
                .docId(docId)
                .mode(mode)
                .question(question)
                .answer(result == null ? null : result.getAnswer())
                .rewrittenQuery(result == null ? null : result.getRewrittenQuery())
                .retrievalScope(result == null ? null : result.getRetrievalScope())
                .finalContext(result == null ? null : result.getFinalContext())
                .retrievedChunks(result == null ? null : result.getRetrievedChunks())
                .retrievedChunkDetails(result == null ? null : result.getRetrievedChunkDetails())
                .status(status)
                .errorMessage(errorMessage)
                .build());
    }

    private void validateWorkspace(String workspaceId) {
        if (!documentWorkspaceRepository.existsWorkspace(workspaceId)) {
            throw new IllegalArgumentException("document workspace not found: " + workspaceId);
        }
    }

    private void validateDocument(String workspaceId, String docId) {
        if (!StringUtils.hasText(docId)) {
            return;
        }
        if (!documentWorkspaceRepository.existsDocument(workspaceId, docId)) {
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
        return new HashSet<>(documentWorkspaceRepository.queryActiveDocumentIds(workspaceId));
    }

    private String buildFilterExpression(String workspaceId, String docId) {
        return StringUtils.hasText(docId)
                ? DocumentWorkspaceMetadataSupport.buildDocumentFilterExpression(workspaceId, docId)
                : DocumentWorkspaceMetadataSupport.buildWorkspaceFilterExpression(workspaceId);
    }

    private String resolveRewrittenQuery(String taskType, String workspaceId, String docId, String question) {
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException("document question is empty");
        }
        try {
            DocumentQueryRewriteResultEntity result = queryRewriteService.rewrite(DocumentQueryRewriteCommandEntity.builder()
                    .taskType(taskType)
                    .taskParams(buildTaskParams(workspaceId, docId))
                    .originalQuestion(question)
                    .build());
            if (result == null || !StringUtils.hasText(result.getRewrittenQuery())) {
                return question.trim();
            }
            return result.getRewrittenQuery().trim();
        } catch (Exception e) {
            log.warn("document query rewrite degraded, taskType={}, message={}", taskType, e.getMessage());
            return question.trim();
        }
    }

    private Map<String, Object> buildTaskParams(String workspaceId, String docId) {
        Map<String, Object> taskParams = new LinkedHashMap<>();
        taskParams.put("workspaceId", workspaceId);
        if (StringUtils.hasText(docId)) {
            taskParams.put("docId", docId);
        }
        return taskParams;
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

    private String safe(String text) {
        return text == null ? "" : text;
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

    private int normalizeLimit(Integer limit, int defaultValue, int maxValue) {
        if (limit == null || limit <= 0) {
            return defaultValue;
        }
        return Math.min(limit, maxValue);
    }
}
