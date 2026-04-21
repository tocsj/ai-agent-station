package com.tkck.infrastructure.adapter.repository;

import com.alibaba.fastjson.JSON;
import com.tkck.domain.document.adapter.repository.IDocumentWorkspaceRepository;
import com.tkck.domain.document.model.entity.DocumentFileEntity;
import com.tkck.domain.document.model.entity.DocumentRetrievedChunkEntity;
import com.tkck.domain.document.model.entity.DocumentTaskRecordEntity;
import com.tkck.domain.document.model.entity.DocumentWorkspaceDetailEntity;
import com.tkck.domain.document.model.entity.DocumentWorkspaceEntity;
import com.tkck.infrastructure.dao.IDocumentWorkspaceDao;
import com.tkck.infrastructure.dao.po.AiKnowledgeChunk;
import com.tkck.infrastructure.dao.po.AiKnowledgeDocument;
import com.tkck.infrastructure.dao.po.AiKnowledgeSpace;
import com.tkck.infrastructure.dao.po.DocumentTaskRecordPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class DocumentWorkspaceRepository implements IDocumentWorkspaceRepository {

    @Resource
    private IDocumentWorkspaceDao documentWorkspaceDao;

    @Override
    public void saveWorkspace(String workspaceId, String workspaceName, String description) {
        documentWorkspaceDao.insertKnowledgeSpace(AiKnowledgeSpace.builder()
                .spaceId(workspaceId)
                .spaceName(workspaceName)
                .spaceType("document")
                .description(description)
                .status(1)
                .build());
    }

    @Override
    public List<DocumentWorkspaceEntity> queryWorkspaceList() {
        return documentWorkspaceDao.queryWorkspaceList().stream()
                .map(this::toWorkspaceEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void saveDocument(DocumentFileEntity documentFileEntity) {
        documentWorkspaceDao.insertKnowledgeDocument(AiKnowledgeDocument.builder()
                .docId(documentFileEntity.getDocId())
                .spaceId(documentFileEntity.getWorkspaceId())
                .fileName(documentFileEntity.getFileName())
                .fileType(documentFileEntity.getFileType())
                .fileSize(documentFileEntity.getFileSize())
                .parseStatus(documentFileEntity.getParseStatus())
                .chunkCount(documentFileEntity.getChunkCount())
                .vectorStatus(documentFileEntity.getVectorStatus())
                .status(1)
                .build());
    }

    @Override
    public void saveChunk(String chunkId, String docId, String workspaceId, Integer chunkIndex, String chunkText, String metadataJson) {
        documentWorkspaceDao.insertKnowledgeChunk(AiKnowledgeChunk.builder()
                .chunkId(chunkId)
                .docId(docId)
                .spaceId(workspaceId)
                .chunkIndex(chunkIndex)
                .chunkText(chunkText)
                .metadataJson(metadataJson)
                .build());
    }

    @Override
    public void updateDocumentParseResult(String docId, String parseStatus, Integer chunkCount, String vectorStatus) {
        documentWorkspaceDao.updateKnowledgeDocumentParseResult(docId, parseStatus, chunkCount, vectorStatus);
    }

    @Override
    public DocumentWorkspaceDetailEntity queryWorkspaceDetail(String workspaceId) {
        AiKnowledgeSpace workspace = documentWorkspaceDao.queryKnowledgeSpaceBySpaceId(workspaceId);
        if (workspace == null) {
            return null;
        }
        List<DocumentFileEntity> documents = new ArrayList<>();
        for (AiKnowledgeDocument document : documentWorkspaceDao.queryKnowledgeDocumentsBySpaceId(workspaceId)) {
            documents.add(toDocumentFileEntity(document));
        }
        return DocumentWorkspaceDetailEntity.builder()
                .workspaceId(workspace.getSpaceId())
                .workspaceName(workspace.getSpaceName())
                .description(workspace.getDescription())
                .status(String.valueOf(workspace.getStatus()))
                .documentCount(documents.size())
                .documents(documents)
                .build();
    }

    @Override
    public boolean existsWorkspace(String workspaceId) {
        Integer count = documentWorkspaceDao.countWorkspace(workspaceId);
        return count != null && count > 0;
    }

    @Override
    public boolean existsDocument(String workspaceId, String docId) {
        Integer count = documentWorkspaceDao.countDocument(workspaceId, docId);
        return count != null && count > 0;
    }

    @Override
    public void saveTaskRecord(DocumentTaskRecordEntity record) {
        documentWorkspaceDao.insertDocumentTaskRecord(DocumentTaskRecordPO.builder()
                .workspaceId(record.getWorkspaceId())
                .docId(record.getDocId())
                .mode(record.getMode())
                .question(record.getQuestion())
                .answer(record.getAnswer())
                .rewrittenQuery(record.getRewrittenQuery())
                .retrievalScope(record.getRetrievalScope())
                .finalContext(record.getFinalContext())
                .retrievedChunksJson(record.getRetrievedChunks() == null ? null : JSON.toJSONString(record.getRetrievedChunks()))
                .retrievedChunkDetailsJson(record.getRetrievedChunkDetails() == null ? null : JSON.toJSONString(record.getRetrievedChunkDetails()))
                .status(record.getStatus())
                .errorMessage(record.getErrorMessage())
                .build());
    }

    @Override
    public List<DocumentTaskRecordEntity> queryRecentTasks(String workspaceId, int limit) {
        return documentWorkspaceDao.queryRecentTaskRecords(workspaceId, limit).stream()
                .map(this::toTaskRecordEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> queryActiveDocumentIds(String workspaceId) {
        return new LinkedHashSet<>(documentWorkspaceDao.queryActiveDocumentIds(workspaceId));
    }

    private DocumentWorkspaceEntity toWorkspaceEntity(AiKnowledgeSpace row) {
        return DocumentWorkspaceEntity.builder()
                .workspaceId(row.getSpaceId())
                .workspaceName(row.getSpaceName())
                .description(row.getDescription())
                .status(String.valueOf(row.getStatus()))
                .documentCount(row.getDocumentCount() == null ? 0 : row.getDocumentCount())
                .updateTime(row.getUpdateTime() == null ? null : String.valueOf(row.getUpdateTime()))
                .build();
    }

    private DocumentFileEntity toDocumentFileEntity(AiKnowledgeDocument row) {
        return DocumentFileEntity.builder()
                .docId(row.getDocId())
                .workspaceId(row.getSpaceId())
                .fileName(row.getFileName())
                .fileType(row.getFileType())
                .fileSize(row.getFileSize())
                .parseStatus(row.getParseStatus())
                .chunkCount(row.getChunkCount())
                .vectorStatus(row.getVectorStatus())
                .build();
    }

    private DocumentTaskRecordEntity toTaskRecordEntity(DocumentTaskRecordPO row) {
        return DocumentTaskRecordEntity.builder()
                .taskId(row.getId())
                .workspaceId(row.getWorkspaceId())
                .docId(row.getDocId())
                .mode(row.getMode())
                .question(row.getQuestion())
                .answer(row.getAnswer())
                .rewrittenQuery(row.getRewrittenQuery())
                .retrievalScope(row.getRetrievalScope())
                .finalContext(row.getFinalContext())
                .retrievedChunks(StringUtils.hasText(row.getRetrievedChunksJson()) ? JSON.parseArray(row.getRetrievedChunksJson(), String.class) : List.of())
                .retrievedChunkDetails(StringUtils.hasText(row.getRetrievedChunkDetailsJson()) ? JSON.parseArray(row.getRetrievedChunkDetailsJson(), DocumentRetrievedChunkEntity.class) : List.of())
                .status(row.getStatus())
                .errorMessage(row.getErrorMessage())
                .createTime(row.getCreateTime() == null ? null : String.valueOf(row.getCreateTime()))
                .build();
    }
}
