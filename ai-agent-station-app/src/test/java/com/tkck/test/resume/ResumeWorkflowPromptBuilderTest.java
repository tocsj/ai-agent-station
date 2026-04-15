package com.tkck.test.resume;

import com.tkck.app.resume.ResumeWorkflowPromptBuilder;
import org.junit.Assert;
import org.junit.Test;

public class ResumeWorkflowPromptBuilderTest {

    @Test
    public void should_build_evaluation_message_with_resume_context() {
        String prompt = ResumeWorkflowPromptBuilder.buildEvaluationMessage(7L, 8L, "请评估这份简历");

        Assert.assertTrue(prompt.contains("resumeId=7"));
        Assert.assertTrue(prompt.contains("knowledgeSpaceId=8"));
        Assert.assertTrue(prompt.contains("请评估这份简历"));
    }

    @Test
    public void should_build_interview_answer_message_with_round_context() {
        String prompt = ResumeWorkflowPromptBuilder.buildInterviewAnswerMessage(
                99L,
                2,
                3,
                "请介绍你如何做缓存一致性",
                "我会先更新数据库再删除缓存。");

        Assert.assertTrue(prompt.contains("interviewSessionId=99"));
        Assert.assertTrue(prompt.contains("当前轮次=2/3"));
        Assert.assertTrue(prompt.contains("请介绍你如何做缓存一致性"));
        Assert.assertTrue(prompt.contains("我会先更新数据库再删除缓存。"));
        Assert.assertTrue(prompt.contains("下一轮问题:"));
    }
}
