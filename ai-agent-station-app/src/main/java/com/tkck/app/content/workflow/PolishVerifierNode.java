package com.tkck.app.content.workflow;

import com.tkck.app.content.ContentPromptBuilder;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class PolishVerifierNode extends AbstractContentWorkflowNode {

    @Override
    public int stepNo() {
        return 4;
    }

    @Override
    public String stepName() {
        return "polish";
    }

    @Override
    public ExecutionStage stage() {
        return ExecutionStage.CONTENT_POLISH;
    }

    @Override
    public String apply(ContentWorkflowContext context) {
        String output = generate(ContentPromptBuilder.buildPolishPrompt(
                context.getTask(),
                context.getValue("draft")));
        context.setValue("polished", output);
        return output;
    }
}
