package com.tkck.app.resume;

public class ResumeWorkflowPromptBuilder {

    private ResumeWorkflowPromptBuilder() {
    }

    public static String buildEvaluationMessage(Long resumeId, Long knowledgeSpaceId, String question) {
        String actualQuestion = (question == null || question.isBlank())
                ? "请基于候选人简历知识空间输出结构化简历评估报告。"
                : question;
        return """
                你正在处理简历评估任务。
                上下文:
                - resumeId=%d
                - knowledgeSpaceId=%d
                要求:
                1. 基于知识空间做评估，不要泛泛而谈。
                2. 输出岗位匹配度、优势、风险点、修改建议。
                3. 结论必须具体、可执行。

                用户问题:
                %s
                """.formatted(resumeId, knowledgeSpaceId, actualQuestion);
    }

    public static String buildInterviewOpeningPrompt(Long resumeId, Long knowledgeSpaceId, String resumeText) {
        return """
                你是一名技术面试官。请基于这份候选人简历生成 3-5 个由浅入深的面试问题。
                上下文:
                - resumeId=%d
                - knowledgeSpaceId=%d

                简历摘要:
                %s

                输出要求:
                1. 每个问题都要贴合简历经历。
                2. 按难度递进。
                3. 直接输出问题列表。
                """.formatted(resumeId, knowledgeSpaceId, resumeText);
    }

    public static String buildInterviewAnswerMessage(Long interviewSessionId,
                                                     Integer roundNo,
                                                     String question,
                                                     String answer) {
        return """
                你正在处理模拟面试回答评估任务。
                上下文:
                - interviewSessionId=%d
                - 第%d轮

                当前题目:
                %s

                候选人回答:
                %s

                输出要求:
                1. 评价回答质量、知识点覆盖和表达问题。
                2. 指出风险点和改进建议。
                3. 给出下一题或下一步追问方向。
                """.formatted(interviewSessionId, roundNo, question, answer);
    }
}
