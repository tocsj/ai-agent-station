package com.tkck.domain.resume.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumeEvaluationTaskEntity {

    private Long taskId;

    private Long resumeId;

    private Long knowledgeSpaceId;

    private String sessionId;

    private String question;

    private String status;

    private String report;

    private String traceId;

    private String errorMessage;

    private String createTime;

    private String updateTime;
}
