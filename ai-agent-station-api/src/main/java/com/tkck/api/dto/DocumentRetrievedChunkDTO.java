package com.tkck.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentRetrievedChunkDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String workspaceId;
    private String docId;
    private String fileName;
    private String chunkIndex;
    private String preview;
}
