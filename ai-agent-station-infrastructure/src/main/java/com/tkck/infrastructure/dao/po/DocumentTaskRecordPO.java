package com.tkck.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentTaskRecordPO {

    private Long id;
    private String workspaceId;
    private String docId;
    private String mode;
    private String question;
    private String answer;
    private String rewrittenQuery;
    private String retrievalScope;
    private String finalContext;
    private String retrievedChunksJson;
    private String retrievedChunkDetailsJson;
    private String status;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
