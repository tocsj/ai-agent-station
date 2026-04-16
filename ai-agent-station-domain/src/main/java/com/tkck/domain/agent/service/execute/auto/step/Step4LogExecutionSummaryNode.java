package com.tkck.domain.agent.service.execute.auto.step;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.tkck.domain.agent.model.entity.AutoAgentExecuteResultEntity;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.model.valobj.AiAgentClientFlowConfigVO;
import com.tkck.domain.agent.model.valobj.enums.AiClientTypeEnumVO;
import com.tkck.domain.agent.service.execute.auto.step.factory.DefaultAutoAgentExecuteStrategyFactory;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionFailure;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class Step4LogExecutionSummaryNode extends AbstractExecuteSupport {

    @Override
    protected String doApply(ExecuteCommandEntity requestParameter,
                             DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("\n=== 执行第 {} 步 ===", dynamicContext.getStep());
        log.info("\n阶段4: 执行总结分析");

        logExecutionSummary(dynamicContext.getMaxStep(), dynamicContext.getExecutionHistory(), dynamicContext.isCompleted());
        generateFinalReport(requestParameter, dynamicContext);

        log.info("\n=== 动态多轮执行结束 ===");
        return "ai agent execution summary completed!";
    }

    @Override
    public StrategyHandler<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> get(
            ExecuteCommandEntity requestParameter,
            DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) {
        return defaultStrategyHandler;
    }

    private void logExecutionSummary(int maxSteps, StringBuilder executionHistory, boolean completed) {
        log.info("\n=== 动态多轮执行总结 ===");
        int actualSteps = Math.min(maxSteps, executionHistory.toString().split("=== 第").length - 1);
        log.info("总执行步数: {}", actualSteps);
        log.info("任务完成状态: {}", completed ? "已完成" : "未完成");
        double efficiency = maxSteps <= 0 ? 100.0 : (double) actualSteps / maxSteps * 100;
        log.info("执行效率: {}%", efficiency);
    }

    private void generateFinalReport(ExecuteCommandEntity requestParameter,
                                     DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        boolean completed = dynamicContext.isCompleted();
        log.info("\n--- 生成{}任务的最终答案 ---", completed ? "已完成" : "未完成");

        String summaryPrompt = getSummaryPrompt(requestParameter, dynamicContext, completed);
        AiAgentClientFlowConfigVO flowConfig =
                dynamicContext.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.RESPONSE_ASSISTANT.getCode());
        ChatClient chatClient = getChatClientByClientId(flowConfig.getClientId());
        long stageStart = System.currentTimeMillis();
        String location = getClass().getSimpleName() + "#generateFinalReport";

        String summaryResult = executeStage(
                ExecutionStage.STEP4_SUMMARIZE,
                requestParameter,
                dynamicContext,
                () -> callChatClientWithAudit(
                        requestParameter,
                        dynamicContext,
                        ExecutionStage.STEP4_SUMMARIZE,
                        "step4_summarize",
                        flowConfig.getClientId(),
                        location,
                        () -> chatClient
                                .prompt(summaryPrompt)
                                .advisors(a -> {
                                    a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, requestParameter.getSessionId() + "-summary")
                                            .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 256);
                                    if (StringUtils.hasText(requestParameter.getQaFilterExpression())) {
                                        a.param(QA_FILTER_EXPRESSION_KEY, requestParameter.getQaFilterExpression());
                                    }
                                })
                                .call()
                                .chatResponse()
                ),
                failure -> buildSummaryFallback(requestParameter, dynamicContext, failure)
        );
        recordStageMetric(requestParameter, dynamicContext, ExecutionStage.STEP4_SUMMARIZE, "step4_summarize", flowConfig.getClientId(), location, System.currentTimeMillis() - stageStart);

        dynamicContext.setValue("finalSummary", summaryResult);
        logFinalReport(dynamicContext, summaryResult, requestParameter.getSessionId());
    }

    private static String getSummaryPrompt(ExecuteCommandEntity requestParameter,
                                           DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                           boolean completed) {
        if (requestParameter.getInterviewSessionId() != null) {
            return getInterviewSummaryPrompt(requestParameter, dynamicContext);
        }

        if (completed) {
            return """
                    基于以下执行过程，请输出最终结果。
                    用户原始问题:
                    %s

                    执行历史:
                    %s

                    输出要求:
                    1. 直接回答用户问题，不要重复过程。
                    2. 使用 Markdown 输出。
                    3. 总字数控制在 400 个中文字符以内。
                    4. 每个一级部分不超过 3 个要点。
                    5. 不要输出多余解释、免责声明或过程复盘。
                    """.formatted(requestParameter.getMessage(), dynamicContext.getExecutionHistory());
        }

        return """
                任务尚未完全完成，但请基于已有执行过程给出当前最可靠的结果。
                用户原始问题:
                %s

                执行历史:
                %s

                输出要求:
                1. 先给出当前可确定的答案。
                2. 再简短说明缺失项和下一步建议。
                3. 使用 Markdown 输出。
                4. 总字数控制在 320 个中文字符以内。
                """.formatted(requestParameter.getMessage(), dynamicContext.getExecutionHistory());
    }

    private static String getInterviewSummaryPrompt(ExecuteCommandEntity requestParameter,
                                                    DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) {
        int roundNo = requestParameter.getInterviewRoundNo() == null ? 1 : requestParameter.getInterviewRoundNo();
        int totalRounds = requestParameter.getInterviewTotalRounds() == null ? 3 : requestParameter.getInterviewTotalRounds();
        boolean finalRound = roundNo >= totalRounds;
        boolean conservative = Boolean.TRUE.equals(dynamicContext.getValue(ExecutionStage.STEP2_EXECUTE.name() + "_degraded"));

        String contract = finalRound
                ? """
                你必须严格按下面结构输出:
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
                你必须严格按下面结构输出:
                本轮评分:
                本轮点评:
                优势:
                薄弱点:
                命中简历片段:
                追问意图:
                下一轮问题:
                面试状态: CONTINUE
                """;

        String extraConstraint = conservative
                ? """
                额外约束:
                1. 当前执行阶段发生过降级，只能输出保守结论。
                2. 不要给出过强判断，必须明确基于已有回答与简历证据。
                3. 如果证据不足，点评和总结必须说明“信息不足，建议继续追问”。
                """
                : "";

        return """
                你正在生成模拟面试闭环结果。
                当前轮次: %d/%d
                用户问题:
                %s

                执行历史:
                %s

                输出约束:
                1. 必须结合执行历史，不要脱离上下文泛化总结。
                2. 本轮评分字段长度不能超过 12 个字符。
                3. 优势、薄弱点、命中简历片段、追问意图都保持短句输出。
                4. 非最后一轮总字数控制在 220 个中文字符以内。
                5. 最后一轮总字数控制在 320 个中文字符以内。
                6. 不要补充任何额外标题、解释或长段落。

                %s
                %s
                """.formatted(roundNo, totalRounds, requestParameter.getMessage(), dynamicContext.getExecutionHistory(), extraConstraint, contract);
    }

    private void logFinalReport(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                String summaryResult,
                                String sessionId) {
        log.info("\n=== 最终总结报告 ===");

        String[] lines = summaryResult.split("\n");
        String currentSection = "summary_overview";
        StringBuilder sectionContent = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            String newSection = detectSummarySection(line);
            if (newSection != null && !newSection.equals(currentSection)) {
                if (!sectionContent.isEmpty()) {
                    sendSummarySubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                }
                currentSection = newSection;
                sectionContent.setLength(0);
            }

            if (!sectionContent.isEmpty()) {
                sectionContent.append("\n");
            }
            sectionContent.append(line);
            log.info("{}", line);
        }

        if (!sectionContent.isEmpty()) {
            sendSummarySubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
        }

        sendSummaryResult(dynamicContext, summaryResult, sessionId);
        sendCompleteResult(dynamicContext, sessionId);
    }

    private void sendSummaryResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                   String summaryResult,
                                   String sessionId) {
        AutoAgentExecuteResultEntity result = AutoAgentExecuteResultEntity.createSummaryResult(summaryResult, sessionId);
        sendSseResult(dynamicContext, result);
    }

    private void sendSummarySubResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                      String subType,
                                      String content,
                                      String sessionId) {
        AutoAgentExecuteResultEntity result = AutoAgentExecuteResultEntity.createSummarySubResult(subType, content, sessionId);
        sendSseResult(dynamicContext, result);
    }

    private void sendCompleteResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext, String sessionId) {
        AutoAgentExecuteResultEntity result = AutoAgentExecuteResultEntity.createCompleteResult(sessionId);
        sendSseResult(dynamicContext, result);
        log.info("已发送完成标识");
    }

    private String detectSummarySection(String content) {
        if (content.contains("本轮评分")) {
            return "score";
        }
        if (content.contains("本轮点评")) {
            return "feedback";
        }
        if (content.contains("优势")) {
            return "strengths";
        }
        if (content.contains("薄弱点")) {
            return "weaknesses";
        }
        if (content.contains("命中简历片段")) {
            return "resume_evidence";
        }
        if (content.contains("追问意图")) {
            return "follow_up_intent";
        }
        if (content.contains("下一轮问题")) {
            return "next_question";
        }
        if (content.contains("最终面试总结")) {
            return "final_report";
        }
        if (content.contains("建议") || content.contains("优化")) {
            return "suggestions";
        }
        if (content.contains("评估") || content.contains("结论")) {
            return "evaluation";
        }
        return null;
    }

    private String buildSummaryFallback(ExecuteCommandEntity requestParameter,
                                        DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                        ExecutionFailure failure) {
        String executionResult = dynamicContext.getValue("executionResult");
        String supervisionResult = dynamicContext.getValue("supervisionResult");
        String degradeReason = failure.getErrorCode() == null ? "总结阶段异常" : failure.getErrorCode().getMessage();

        if (requestParameter.getInterviewSessionId() != null) {
            if (requestParameter.getInterviewRoundNo() != null
                    && requestParameter.getInterviewTotalRounds() != null
                    && requestParameter.getInterviewRoundNo() >= requestParameter.getInterviewTotalRounds()) {
                return """
                        本轮评分: N/A
                        本轮点评: 总结阶段降级，本轮结论仅基于已有执行与质检结果。
                        优势: 已保留候选人本轮回答的核心信息。
                        薄弱点: 建议复核本轮回答与简历证据的一致性。
                        命中简历片段: 请以后续明细查询接口中的简历命中内容为准。
                        追问意图: 当前为终轮，降级输出综合反馈。
                        最终面试总结: 已保留执行结果与质检结果，建议模型恢复后重新生成正式面试总结。
                        面试状态: FINISHED
                        降级原因: %s
                        """.formatted(degradeReason);
            }
            return """
                    本轮评分: N/A
                    本轮点评: 总结阶段降级，本轮结论仅基于已有执行与质检结果。
                    优势: 已保留候选人本轮回答的核心信息。
                    薄弱点: 建议复核本轮回答与简历证据的一致性。
                    命中简历片段: 请以后续明细查询接口中的简历命中内容为准。
                    追问意图: 当前执行阶段降级，下一轮继续围绕薄弱点追问。
                    下一轮问题: 请继续说明你如何在真实项目中验证该方案有效。
                    面试状态: CONTINUE
                    降级原因: %s
                    """.formatted(degradeReason);
        }

        return """
                ## 当前结论
                - 已基于已有执行链结果生成降级总结。
                - 用户问题: %s

                ## 已有依据
                - 执行结果: %s
                - 质检结果: %s

                ## 风险说明
                - 最终总结阶段失败，当前输出未经过完整总结模型重写。
                - 降级原因: %s
                """.formatted(requestParameter.getMessage(),
                executionResult == null ? "暂无" : executionResult,
                supervisionResult == null ? "暂无" : supervisionResult,
                degradeReason);
    }
}
