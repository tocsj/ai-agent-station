package com.tkck.domain.workbench.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkbenchRecentRunEntity {

    private String traceId;
    private String displayTaskId;
    private String taskId;
    private String taskType;
    private String taskTypeName;
    private String taskSubType;
    private String taskSubTypeName;
    private Long durationMs;
    private Long totalTokens;
    private String status;
    private String statusText;
    private String lastTime;
    private String detailTraceId;
}
