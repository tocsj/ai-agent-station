package com.tkck.test.content;

import com.tkck.app.content.workflow.ContentWorkflowContext;
import com.tkck.app.content.workflow.PublishPlannerNode;
import com.tkck.domain.content.model.entity.ContentTaskEntity;
import org.junit.Assert;
import org.junit.Test;

public class PublishPlannerNodeTest {

    @Test
    public void shouldAlwaysCreateSaveDraftCommandForConfiguredDraftChannel() {
        PublishPlannerNode node = new PublishPlannerNode();
        ContentWorkflowContext context = ContentWorkflowContext.builder()
                .task(ContentTaskEntity.builder()
                        .taskId(30L)
                        .topic("企业级 AI Agent 平台")
                        .channel("devto")
                        .keywords("java,ai,agent")
                        .build())
                .build();
        context.setValue("topicPlan", "企业级 AI Agent 平台实战\n- 选题说明");
        context.setValue("polished", "这是一篇中文正文。");
        context.setValue("compliance", "结论: REVISE\n风险点: 需要人工复核");

        String output = node.apply(context);

        Assert.assertEquals("save_draft", context.getPublishCommand().getAction());
        Assert.assertEquals("devto", context.getPublishCommand().getChannel());
        Assert.assertTrue(output.contains("action=save_draft"));
    }
}
