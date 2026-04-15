package com.tkck.test.content;

import com.tkck.app.content.ContentPromptBuilder;
import com.tkck.domain.content.model.entity.ContentTaskEntity;
import org.junit.Assert;
import org.junit.Test;

public class ContentPromptBuilderTest {

    @Test
    public void shouldConstrainContentPromptsToChineseAndNodeLength() {
        ContentTaskEntity task = ContentTaskEntity.builder()
                .topic("企业级 AI Agent 平台")
                .platform("Dev.to")
                .style("专业")
                .keywords("java,ai,agent")
                .build();

        Assert.assertTrue(ContentPromptBuilder.buildTopicPrompt(task).contains("必须使用中文"));
        Assert.assertTrue(ContentPromptBuilder.buildTopicPrompt(task).contains("200字以内"));
        Assert.assertTrue(ContentPromptBuilder.buildOutlinePrompt(task, "选题").contains("600字以内"));
        Assert.assertTrue(ContentPromptBuilder.buildDraftPrompt(task, "选题", "大纲").contains("1200-1800字"));
        Assert.assertTrue(ContentPromptBuilder.buildCompliancePrompt(task, "正文").contains("400字以内"));
        Assert.assertTrue(ContentPromptBuilder.buildPublishSummaryPrompt(task, "正文", "合规", "结果").contains("300字以内"));
    }
}
