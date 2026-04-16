package com.tkck.domain.document.service;

import com.tkck.domain.document.model.entity.DocumentFileEntity;
import com.tkck.domain.document.model.entity.DocumentTaskRecordEntity;
import com.tkck.domain.document.model.entity.DocumentTaskResultEntity;
import com.tkck.domain.document.model.entity.DocumentWorkspaceDetailEntity;
import com.tkck.domain.document.model.entity.DocumentWorkspaceEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IDocumentWorkspaceService {

    DocumentWorkspaceEntity createWorkspace(String workspaceName, String description);

    List<DocumentWorkspaceEntity> listWorkspaces();

    DocumentWorkspaceDetailEntity queryActiveWorkspace();

    DocumentFileEntity upload(String workspaceId, MultipartFile file) throws Exception;

    DocumentWorkspaceDetailEntity queryWorkspaceDetail(String workspaceId);

    DocumentTaskResultEntity ask(String workspaceId, String docId, String question);

    DocumentTaskResultEntity summary(String workspaceId, String docId, String summaryMode);

    DocumentTaskResultEntity followup(String workspaceId, String docId, String perspective);

    DocumentTaskResultEntity quiz(String workspaceId, String docId, Integer questionCount, String quizType);

    List<DocumentTaskRecordEntity> queryRecentTasks(String workspaceId, Integer limit);
}
