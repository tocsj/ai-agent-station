package com.tkck.app.content.workflow;

import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;
import com.tkck.domain.content.model.entity.PublishCommandEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(6)
@Slf4j
public class PublishPlannerNode extends AbstractContentWorkflowNode {

    @Override
    public int stepNo() {
        return 6;
    }

    @Override
    public String stepName() {
        return "publish_plan";
    }

    @Override
    public ExecutionStage stage() {
        return ExecutionStage.CONTENT_PUBLISH_PLAN;
    }

    @Override
    public String apply(ContentWorkflowContext context) {
        String topicPlan = safe(context.getValue("topicPlan"));
        String title = topicPlan.contains("\n") ? topicPlan.substring(0, topicPlan.indexOf('\n')).trim() : topicPlan.trim();
        if (title.isBlank()) {
            title = context.getTask().getTopic();
        }
        String action = "save_draft";
        PublishCommandEntity command = PublishCommandEntity.builder()
                .channel(context.getTask().getChannel())
                .action(action)
                .title(title)
                .summary(title)
                .content(safe(context.getValue("polished")))
                .tags(safe(context.getTask().getKeywords()))
                .taskId(context.getTask().getTaskId())
                .build();
        context.setPublishCommand(command);
        context.setValue("publishTitle", title);
        log.info("内容发布规划完成, taskId={}, action={}, channel={}, title={}",
                context.getTask().getTaskId(), action, command.getChannel(), title);
        return "action=" + action + "\ntitle=" + title + "\ntags=" + safe(context.getTask().getKeywords());
    }
}
