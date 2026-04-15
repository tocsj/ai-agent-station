package com.tkck.app.content.workflow;

import com.tkck.app.content.ContentPromptBuilder;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class ComplianceReviewerNode extends AbstractContentWorkflowNode {

    @Override
    public int stepNo() {
        return 5;
    }

    @Override
    public String stepName() {
        return "compliance";
    }

    @Override
    public ExecutionStage stage() {
        return ExecutionStage.CONTENT_COMPLIANCE;
    }

    @Override
    protected String clientId() {
        return "5302";
    }

    @Override
    public String apply(ContentWorkflowContext context) {
        String output = generate(ContentPromptBuilder.buildCompliancePrompt(
                context.getTask(),
                context.getValue("polished")));
        context.setValue("compliance", output);
        return output;
    }
}
