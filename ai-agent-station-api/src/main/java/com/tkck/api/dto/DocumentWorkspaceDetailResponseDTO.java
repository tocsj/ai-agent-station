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
public class DocumentWorkspaceDetailResponseDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String workspaceId;
    private String workspaceName;
    private String description;
    private String status;
    private Integer documentCount;
    private List<DocumentItem> documents;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String docId;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String parseStatus;
        private Integer chunkCount;
        private String vectorStatus;
    }
}
