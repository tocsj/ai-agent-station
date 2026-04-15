package com.tkck.domain.document.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentFileEntity {

    private String docId;

    private String workspaceId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String parseStatus;

    private Integer chunkCount;

    private String vectorStatus;
}
