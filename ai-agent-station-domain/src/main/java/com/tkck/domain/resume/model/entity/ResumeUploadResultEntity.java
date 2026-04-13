package com.tkck.domain.resume.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumeUploadResultEntity {

    private Long resumeId;

    private Long knowledgeSpaceId;

    private String knowledgeTag;

    private String fileName;

    private Integer chunkCount;
}
