package com.tkck.app.document;

public final class DocumentWorkspacePromptBuilder {

    private DocumentWorkspacePromptBuilder() {
    }

    public static String buildAskPrompt(String workspaceId, String docId, String question) {
        return """
                你正在处理文档知识助手问答任务。
                上下文:
                - workspaceId=%s
                - docId=%s

                用户问题:
                %s

                输出要求:
                1. 必须基于检索到的文档内容回答。
                2. 如果证据不足，要明确说明。
                3. 结论尽量结构化、简洁。
                """.formatted(workspaceId, docId == null ? "ALL" : docId, question);
    }

    public static String buildSummaryPrompt(String workspaceId, String docId, String summaryMode) {
        return """
                你正在处理文档摘要任务。
                上下文:
                - workspaceId=%s
                - docId=%s
                - 摘要类型=%s

                输出要求:
                1. 基于文档内容输出摘要。
                2. 根据摘要类型组织结果。
                3. 不要脱离文档泛化发挥。
                """.formatted(workspaceId, docId == null ? "ALL" : docId, summaryMode);
    }

    public static String buildFollowupPrompt(String workspaceId, String docId, String perspective) {
        return """
                你正在处理文档追问生成任务。
                上下文:
                - workspaceId=%s
                - docId=%s
                - 追问视角=%s

                输出要求:
                1. 生成 3 到 5 个高质量追问问题。
                2. 问题必须能帮助用户继续理解文档。
                3. 不要输出解释性长段落。
                """.formatted(workspaceId, docId == null ? "ALL" : docId, perspective);
    }

    public static String buildQuizPrompt(String workspaceId, String docId, Integer questionCount, String quizType) {
        return """
                你正在处理文档测验生成任务。
                上下文:
                - workspaceId=%s
                - docId=%s
                - 题目数量=%d
                - 题型=%s

                输出要求:
                1. 题目必须基于文档内容。
                2. 数量和题型必须符合要求。
                3. 优先输出可直接展示的题目列表。
                """.formatted(workspaceId, docId == null ? "ALL" : docId, questionCount == null ? 5 : questionCount, quizType);
    }
}
