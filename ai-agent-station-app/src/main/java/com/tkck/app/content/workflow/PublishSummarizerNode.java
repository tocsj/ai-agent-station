package com.tkck.app.content.workflow;

import com.tkck.app.content.ContentPromptBuilder;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(8)
public class PublishSummarizerNode extends AbstractContentWorkflowNode {

    @Override
    public int stepNo() {
        return 8;
    }

    @Override
    public String stepName() {
        return "publish_summary";
    }

    @Override
    public ExecutionStage stage() {
        return ExecutionStage.CONTENT_PUBLISH_SUMMARY;
    }

    @Override
    public String apply(ContentWorkflowContext context) {
        String output = generate(ContentPromptBuilder.buildPublishSummaryPrompt(
                context.getTask(),
                safe(context.getValue("polished")),
                safe(context.getValue("compliance")),
                context.getPublishResult() == null ? "" : safe(context.getPublishResult().getMessage())));
        context.setValue("summary", output);
        return output;
    }
}
