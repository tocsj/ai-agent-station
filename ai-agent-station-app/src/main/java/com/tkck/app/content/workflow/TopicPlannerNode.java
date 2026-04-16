package com.tkck.app.content.workflow;

import com.tkck.app.content.ContentPromptBuilder;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class TopicPlannerNode extends AbstractContentWorkflowNode {

    @Override
    public int stepNo() {
        return 1;
    }

    @Override
    public String stepName() {
        return "topic_plan";
    }

    @Override
    public ExecutionStage stage() {
        return ExecutionStage.CONTENT_TOPIC_PLAN;
    }

    @Override
    public String apply(ContentWorkflowContext context) {
        String output = generate(context, ContentPromptBuilder.buildTopicPrompt(context.getTask()));
        context.setValue("topicPlan", output);
        return output;
    }
}
