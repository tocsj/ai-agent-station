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
public class AiKnowledgeDocument {

    private Long id;
    private String docId;
    private String spaceId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String parseStatus;
    private Integer chunkCount;
    private String vectorStatus;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
