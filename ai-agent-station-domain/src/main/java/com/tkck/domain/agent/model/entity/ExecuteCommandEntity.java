package com.tkck.domain.agent.model.entity;

import com.tkck.domain.agent.model.valobj.ExecutionMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行命令实体
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/27 16:46
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteCommandEntity {

    private String aiAgentId;

    private String taskType;

    private String subType;

    private ExecutionMode executionMode;

    private String message;

    private String sessionId;

    private Integer maxStep;

    private String qaFilterExpression;

    private Long resumeId;

    private Long knowledgeSpaceId;

    private Long resumeEvaluationTaskId;

    private Long interviewSessionId;

    private Integer interviewRoundNo;

    private Integer interviewTotalRounds;

    private Long contentTaskId;

}
