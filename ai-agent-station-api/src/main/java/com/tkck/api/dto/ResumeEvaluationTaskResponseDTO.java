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
public class ResumeEvaluationTaskResponseDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
