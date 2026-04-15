package com.tkck.app.content;

import com.tkck.domain.content.model.entity.ContentTaskEntity;

public final class ContentPromptBuilder {

    private static final String GLOBAL_RULES = """

            全局要求：
            - 必须使用中文回复，除平台名、技术名词、固定字段值外，不要输出英文段落。
            - 不要解释你正在做什么，不要输出自我说明。
            - 按要求控制长度，内容要可直接展示给用户。
            """;

    private ContentPromptBuilder() {
    }

    public static String buildTopicPrompt(ContentTaskEntity task) {
        return """
                你是企业级内容策划助手。请围绕主题生成适合发布的标题和选题方向。
                主题：%s
                平台：%s
                风格：%s
                关键词：%s
                输出要求：
                1. 第一行只输出一个中文主标题。
                2. 再给出 3 条选题要点。
                3. 总长度控制在 200字以内。
                %s
                """.formatted(task.getTopic(), task.getPlatform(), safe(task.getStyle()), safe(task.getKeywords()), GLOBAL_RULES);
    }

    public static String buildOutlinePrompt(ContentTaskEntity task, String topicPlan) {
        return """
                请基于选题结果生成结构化中文文章大纲。
                主题：%s
                平台：%s
                选题结果：
                %s
                输出要求：
                1. 生成 5 到 6 个一级标题。
                2. 每个标题补充一句写作说明。
                3. 总长度控制在 600字以内。
                %s
                """.formatted(task.getTopic(), task.getPlatform(), safe(topicPlan), GLOBAL_RULES);
    }

    public static String buildDraftPrompt(ContentTaskEntity task, String topicPlan, String outline) {
        return """
                请根据选题和大纲生成中文文章初稿。
                主题：%s
                平台：%s
                选题结果：
                %s
                大纲：
                %s
                输出要求：
                1. 输出完整正文，结构清晰，可直接作为草稿保存。
                2. 内容长度控制在 1200-1800字。
                3. 不要输出英文段落，不要输出写作过程说明。
                %s
                """.formatted(task.getTopic(), task.getPlatform(), safe(topicPlan), safe(outline), GLOBAL_RULES);
    }

    public static String buildPolishPrompt(ContentTaskEntity task, String draft) {
        return """
                请对以下中文文章初稿做润色。
                平台：%s
                风格：%s
                初稿：
                %s
                输出要求：
                1. 只输出润色后的中文正文。
                2. 保持信息不缩水，提升流畅度和专业度。
                3. 长度控制在 1200-1800字。
                %s
                """.formatted(task.getPlatform(), safe(task.getStyle()), safe(draft), GLOBAL_RULES);
    }

    public static String buildCompliancePrompt(ContentTaskEntity task, String polished) {
        return """
                请审核以下内容是否适合保存为草稿。
                平台：%s
                正文：
                %s
                输出要求：
                1. 第一行输出：结论: PASS 或 结论: REVISE。
                2. 再输出风险点和修改建议。
                3. 总长度控制在 400字以内。
                4. 必须使用中文。
                """.formatted(task.getPlatform(), safe(polished));
    }

    public static String buildPublishPlanPrompt(ContentTaskEntity task, String title, String polished, String compliance) {
        return """
                请生成发布计划。
                渠道：%s
                标题：%s
                正文：
                %s
                审核结果：
                %s
                输出要求：
                1. action 只能是 save_draft。
                2. 给出 tags。
                3. 给出一句中文执行说明。
                4. 总长度控制在 200字以内。
                """.formatted(task.getChannel(), safe(title), safe(polished), safe(compliance));
    }

    public static String buildPublishSummaryPrompt(ContentTaskEntity task, String finalContent, String compliance, String publishResult) {
        return """
                请生成内容自动化执行总结。
                主题：%s
                审核：
                %s
                发布结果：
                %s
                正文：
                %s
                输出要求：
                1. 总结执行结果。
                2. 说明是否已保存草稿。
                3. 给出后续建议。
                4. 总长度控制在 300字以内。
                5. 必须使用中文。
                """.formatted(task.getTopic(), safe(compliance), safe(publishResult), safe(finalContent));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
