package com.tkck.test.document;

import com.tkck.app.document.DocumentWorkspacePromptBuilder;
import org.junit.Assert;
import org.junit.Test;

public class DocumentPromptBuilderTest {

    @Test
    public void should_build_ask_prompt_with_workspace_and_doc_scope() {
        String prompt = DocumentWorkspacePromptBuilder.buildAskPrompt(
                "dws_001",
                "doc_001",
                "请总结这份架构文档的核心设计。"
        );

        Assert.assertTrue(prompt.contains("workspaceId=dws_001"));
        Assert.assertTrue(prompt.contains("docId=doc_001"));
        Assert.assertTrue(prompt.contains("请总结这份架构文档的核心设计。"));
    }

    @Test
    public void should_build_summary_prompt_with_mode() {
        String prompt = DocumentWorkspacePromptBuilder.buildSummaryPrompt(
                "dws_001",
                null,
                "risk_todo"
        );

        Assert.assertTrue(prompt.contains("workspaceId=dws_001"));
        Assert.assertTrue(prompt.contains("摘要类型=risk_todo"));
    }

    @Test
    public void should_build_followup_prompt_with_perspective() {
        String prompt = DocumentWorkspacePromptBuilder.buildFollowupPrompt(
                "dws_001",
                null,
                "technical_review"
        );

        Assert.assertTrue(prompt.contains("workspaceId=dws_001"));
        Assert.assertTrue(prompt.contains("追问视角=technical_review"));
    }

    @Test
    public void should_build_quiz_prompt_with_count_and_type() {
        String prompt = DocumentWorkspacePromptBuilder.buildQuizPrompt(
                "dws_001",
                "doc_001",
                5,
                "mixed"
        );

        Assert.assertTrue(prompt.contains("workspaceId=dws_001"));
        Assert.assertTrue(prompt.contains("docId=doc_001"));
        Assert.assertTrue(prompt.contains("题目数量=5"));
        Assert.assertTrue(prompt.contains("题型=mixed"));
    }
}
