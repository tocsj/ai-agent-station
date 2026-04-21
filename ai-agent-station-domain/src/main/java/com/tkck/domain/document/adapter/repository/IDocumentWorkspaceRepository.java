package com.tkck.domain.document.adapter.repository;

import com.tkck.domain.document.model.entity.DocumentFileEntity;
import com.tkck.domain.document.model.entity.DocumentTaskRecordEntity;
import com.tkck.domain.document.model.entity.DocumentWorkspaceDetailEntity;
import com.tkck.domain.document.model.entity.DocumentWorkspaceEntity;

import java.util.List;
import java.util.Set;

public interface IDocumentWorkspaceRepository {

    void saveWorkspace(String workspaceId, String workspaceName, String description);

    List<DocumentWorkspaceEntity> queryWorkspaceList();

    void saveDocument(DocumentFileEntity documentFileEntity);

    void saveChunk(String chunkId, String docId, String workspaceId, Integer chunkIndex, String chunkText, String metadataJson);

    void updateDocumentParseResult(String docId, String parseStatus, Integer chunkCount, String vectorStatus);

    DocumentWorkspaceDetailEntity queryWorkspaceDetail(String workspaceId);

    boolean existsWorkspace(String workspaceId);

    boolean existsDocument(String workspaceId, String docId);

    void saveTaskRecord(DocumentTaskRecordEntity record);

    List<DocumentTaskRecordEntity> queryRecentTasks(String workspaceId, int limit);

    Set<String> queryActiveDocumentIds(String workspaceId);
}
