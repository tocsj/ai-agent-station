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
                1. 必须基于知识空间进行评估，不要泛泛而谈。
                2. 输出岗位匹配度、优势、风险点、修改建议、项目深挖点。
                3. 结论必须具体、可执行，并引用简历中的关键信息。

                用户问题:
                %s
                """.formatted(resumeId, knowledgeSpaceId, actualQuestion);
    }

    public static String buildInterviewOpeningPrompt(Long resumeId, Long knowledgeSpaceId, String resumeText) {
        return """
                你是一名技术面试官。请基于这份候选人简历上下文，只生成 1 个首轮面试问题。
                上下文:
                - resumeId=%d
                - knowledgeSpaceId=%d

                召回片段:
                %s

                输出要求:
                1. 问题必须紧贴候选人真实经历与项目内容，优先围绕项目、职责、技术取舍、问题排查。
                2. 这只是首轮问题，难度要适中，适合先让候选人展开说明。
                3. 不要泛泛而谈，不要问与简历无关的模板题。
                4. 只输出一个问题正文，不要输出编号，不要输出解释。
                """.formatted(resumeId, knowledgeSpaceId, resumeText);
    }

    public static String buildInterviewAnswerMessage(Long interviewSessionId,
                                                     Integer roundNo,
                                                     Integer totalRounds,
                                                     String question,
                                                     String answer) {
        int actualTotalRounds = totalRounds == null || totalRounds <= 0 ? 3 : totalRounds;
        boolean finalRound = roundNo != null && roundNo >= actualTotalRounds;
        String outputContract = finalRound
                ? """
                你必须严格按下面结构输出，不要增加无关段落：
                本轮评分:
                本轮点评:
                优势:
                薄弱点:
                命中简历片段:
                追问意图:
                最终面试总结:
                面试状态: FINISHED
                """
                : """
                你必须严格按下面结构输出，不要增加无关段落：
                本轮评分:
                本轮点评:
                优势:
                薄弱点:
                命中简历片段:
                追问意图:
                下一轮问题:
                面试状态: CONTINUE
                """;

        return """
                你正在处理模拟面试回答评估任务。
                上下文:
                - interviewSessionId=%d
                - 当前轮次=%d/%d

                当前题目:
                %s

                候选人回答:
                %s

                评估要求:
                1. 结合简历知识空间和当前题目评估回答质量，不要脱离上下文泛评。
                2. 评分要体现候选人对问题的覆盖度、技术准确性、表达完整性。
                3. 本轮点评聚焦结论，优势和薄弱点分别只写最关键的一点到两点。
                4. 命中简历片段必须引用与本轮回答最相关的候选人经历，不要虚构。
                5. 追问意图用于说明下一轮为什么这样追问；如果已经是最后一轮，也要说明综合评估意图。
                6. 如果不是最后一轮，下一轮问题必须更深入，并且围绕当前回答暴露的薄弱点。
                7. 如果已经是最后一轮，必须给出可直接用于面试反馈的最终面试总结。
                8. 本轮评分只能输出短格式，如 8/10，长度不超过 12 个字符。
                9. 非最后一轮总输出控制在 220 个中文字符以内；最后一轮控制在 320 个中文字符以内。
                10. 不要输出长段落，不要添加额外标题。

                %s
                """.formatted(interviewSessionId, roundNo, actualTotalRounds, question, answer, outputContract);
    }
}
