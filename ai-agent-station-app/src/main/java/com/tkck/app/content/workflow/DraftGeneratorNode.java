package com.tkck.app.content.workflow;

import com.tkck.app.content.ContentPromptBuilder;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class DraftGeneratorNode extends AbstractContentWorkflowNode {

    @Override
    public int stepNo() {
        return 3;
    }

    @Override
    public String stepName() {
        return "draft";
    }

    @Override
    public ExecutionStage stage() {
        return ExecutionStage.CONTENT_DRAFT;
    }

    @Override
    public String apply(ContentWorkflowContext context) {
        String output = generate(context, ContentPromptBuilder.buildDraftPrompt(
                context.getTask(),
                context.getValue("topicPlan"),
                context.getValue("outline")));
        context.setValue("draft", output);
        return output;
    }
}
