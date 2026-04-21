package com.tkck.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentTaskResultResponseDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String answer;
    private String rewrittenQuery;
    private String retrievalScope;
    private String finalContext;
    private List<String> retrievedChunks;
    private List<DocumentRetrievedChunkDTO> retrievedChunkDetails;
}
