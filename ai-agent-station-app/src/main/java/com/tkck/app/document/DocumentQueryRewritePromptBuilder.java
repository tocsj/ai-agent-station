package com.tkck.app.document;

import com.alibaba.fastjson.JSON;

import java.util.Map;

public class DocumentQueryRewritePromptBuilder {

    private DocumentQueryRewritePromptBuilder() {
    }

    public static String buildPrompt(String taskType, Map<String, Object> taskParams, String originalQuestion) {
        return """
                你是企业知识库检索改写助手。你的任务不是回答问题，而是把用户原始提问改写成更适合向量检索的查询。

                约束：
                1. 不允许引入原问题中不存在的新事实
                2. 不允许改变用户原意
                3. 只允许做术语补全、关键词提炼、指代消解、检索表达归一化
                4. 输出必须是 JSON，且只能输出 JSON

                输入：
                - taskType: %s
                - taskParams: %s
                - originalQuestion: %s

                输出 JSON：
                {
                  "rewrittenQuery": "用于检索的改写结果",
                  "rewriteReason": "一句话说明改写意图"
                }
                """.formatted(
                safe(taskType),
                JSON.toJSONString(taskParams == null ? Map.of() : taskParams),
                safe(originalQuestion)
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
