package com.tkck.domain.document.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentRetrievedChunkEntity {

    private String workspaceId;

    private String docId;

    private String fileName;

    private String chunkIndex;

    private String preview;
}
