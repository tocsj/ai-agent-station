package com.tkck.test.resume;

import com.tkck.app.resume.ResumeWorkflowPromptBuilder;
import org.junit.Assert;
import org.junit.Test;

public class ResumeWorkflowPromptBuilderStructuredInterviewTest {

    @Test
    public void should_require_structured_continue_round_output_contract() {
        String prompt = ResumeWorkflowPromptBuilder.buildInterviewAnswerMessage(
                9L,
                1,
                3,
                "请你介绍一下你在订单系统里做过的缓存设计。",
                "我主要做了 Redis 缓存、热点 Key 隔离和延迟双删。");

        Assert.assertTrue(prompt.contains("本轮评分:"));
        Assert.assertTrue(prompt.contains("本轮点评:"));
        Assert.assertTrue(prompt.contains("优势:"));
        Assert.assertTrue(prompt.contains("薄弱点:"));
        Assert.assertTrue(prompt.contains("命中简历片段:"));
        Assert.assertTrue(prompt.contains("追问意图:"));
        Assert.assertTrue(prompt.contains("下一轮问题:"));
        Assert.assertTrue(prompt.contains("面试状态: CONTINUE"));
    }

    @Test
    public void should_require_structured_final_round_output_contract() {
        String prompt = ResumeWorkflowPromptBuilder.buildInterviewAnswerMessage(
                9L,
                3,
                3,
                "最后请总结一下你做这个项目最体现工程能力的部分。",
                "我主要负责架构拆分、压测和容器化交付。");

        Assert.assertTrue(prompt.contains("本轮评分:"));
        Assert.assertTrue(prompt.contains("本轮点评:"));
        Assert.assertTrue(prompt.contains("优势:"));
        Assert.assertTrue(prompt.contains("薄弱点:"));
        Assert.assertTrue(prompt.contains("命中简历片段:"));
        Assert.assertTrue(prompt.contains("追问意图:"));
        Assert.assertTrue(prompt.contains("最终面试总结:"));
        Assert.assertTrue(prompt.contains("面试状态: FINISHED"));
    }
}
