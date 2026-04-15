package com.tkck.app.content;

import com.tkck.domain.content.model.entity.ContentTaskEntity;

public final class ContentPromptBuilder {

    private ContentPromptBuilder() {
    }

    public static String buildTopicPrompt(ContentTaskEntity task) {
        return "你是企业级内容策划助手。请围绕以下主题生成一个适合发布的标题方案和选题方向。\n"
                + "主题:" + task.getTopic() + "\n"
                + "平台:" + task.getPlatform() + "\n"
                + "风格:" + safe(task.getStyle()) + "\n"
                + "关键词:" + safe(task.getKeywords()) + "\n"
                + "输出要求:\n"
                + "1. 第一行只输出一个主标题\n"
                + "2. 再给出3条选题要点\n"
                + "3. 保持简洁专业";
    }

    public static String buildOutlinePrompt(ContentTaskEntity task, String topicPlan) {
        return "请基于以下内容生成结构化文章大纲。\n"
                + "主题:" + task.getTopic() + "\n"
                + "平台:" + task.getPlatform() + "\n"
                + "选题结果:\n" + safe(topicPlan) + "\n"
                + "输出要求:\n"
                + "1. 生成5到7个一级标题\n"
                + "2. 每个标题补一行说明";
    }

    public static String buildDraftPrompt(ContentTaskEntity task, String topicPlan, String outline) {
        return "请根据以下内容生成文章初稿。\n"
                + "主题:" + task.getTopic() + "\n"
                + "平台:" + task.getPlatform() + "\n"
                + "选题结果:\n" + safe(topicPlan) + "\n"
                + "大纲:\n" + safe(outline) + "\n"
                + "输出要求:\n"
                + "1. 输出完整正文\n"
                + "2. 保持结构清晰\n"
                + "3. 不要出现自我解释";
    }

    public static String buildPolishPrompt(ContentTaskEntity task, String draft) {
        return "请对以下文章初稿做润色。\n"
                + "平台:" + task.getPlatform() + "\n"
                + "风格:" + safe(task.getStyle()) + "\n"
                + "初稿:\n" + safe(draft) + "\n"
                + "输出要求:\n"
                + "1. 提升流畅度\n"
                + "2. 保持信息不缩水\n"
                + "3. 输出润色后的正文";
    }

    public static String buildCompliancePrompt(ContentTaskEntity task, String polished) {
        return "请审核以下内容是否适合保存为草稿。\n"
                + "平台:" + task.getPlatform() + "\n"
                + "正文:\n" + safe(polished) + "\n"
                + "输出要求:\n"
                + "1. 结论: PASS 或 REVISE\n"
                + "2. 风险点\n"
                + "3. 修改建议";
    }

    public static String buildPublishPlanPrompt(ContentTaskEntity task, String title, String polished, String compliance) {
        return "请生成发布计划。\n"
                + "渠道:" + task.getChannel() + "\n"
                + "标题:" + safe(title) + "\n"
                + "正文:\n" + safe(polished) + "\n"
                + "审核结果:\n" + safe(compliance) + "\n"
                + "输出要求:\n"
                + "1. action 只能是 save_draft 或 block\n"
                + "2. 给出 tags\n"
                + "3. 给出一句执行说明";
    }

    public static String buildPublishSummaryPrompt(ContentTaskEntity task, String finalContent, String compliance, String publishResult) {
        return "请生成内容自动化执行总结。\n"
                + "主题:" + task.getTopic() + "\n"
                + "审核:\n" + safe(compliance) + "\n"
                + "发布结果:\n" + safe(publishResult) + "\n"
                + "正文:\n" + safe(finalContent) + "\n"
                + "输出要求:\n"
                + "1. 总结执行结果\n"
                + "2. 说明是否已保存草稿\n"
                + "3. 给出后续建议";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
