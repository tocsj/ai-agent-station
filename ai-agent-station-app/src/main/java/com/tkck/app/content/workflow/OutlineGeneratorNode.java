package com.tkck.app.content.workflow;

import com.tkck.app.content.ContentPromptBuilder;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class OutlineGeneratorNode extends AbstractContentWorkflowNode {

    @Override
    public int stepNo() {
        return 2;
    }

    @Override
    public String stepName() {
        return "outline";
    }

    @Override
    public ExecutionStage stage() {
        return ExecutionStage.CONTENT_OUTLINE;
    }

    @Override
    public String apply(ContentWorkflowContext context) {
        String output = generate(ContentPromptBuilder.buildOutlinePrompt(
                context.getTask(),
                context.getValue("topicPlan")));
        context.setValue("outline", output);
        return output;
    }
}
