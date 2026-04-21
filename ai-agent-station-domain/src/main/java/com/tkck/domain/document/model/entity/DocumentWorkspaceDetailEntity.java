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
public class DocumentWorkspaceDetailEntity {

    private String workspaceId;

    private String workspaceName;

    private String description;

    private String status;

    private Integer documentCount;

    private List<DocumentFileEntity> documents;
}
