package com.tkck.app.content.workflow;

import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;

public interface ContentWorkflowNode {

    int stepNo();

    String stepName();

    ExecutionStage stage();

    String apply(ContentWorkflowContext context) throws Exception;
}
