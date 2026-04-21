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
public class AiKnowledgeChunk {

    private Long id;
    private String chunkId;
    private String docId;
    private String spaceId;
    private Integer chunkIndex;
    private String chunkText;
    private String metadataJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
