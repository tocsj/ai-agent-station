package com.tkck.domain.document.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentTaskRecordEntity {

    private Long taskId;

    private String workspaceId;

    private String docId;

    private String mode;

    private String question;

    private String answer;

    private String rewrittenQuery;

    private String retrievalScope;

    private String finalContext;

    private List<String> retrievedChunks;

    private List<DocumentRetrievedChunkEntity> retrievedChunkDetails;

    private String status;

    private String errorMessage;

    private String createTime;
}
