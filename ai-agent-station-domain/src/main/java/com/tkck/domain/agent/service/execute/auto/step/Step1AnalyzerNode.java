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
public class Step1AnalyzerNode extends AbstractExecuteSupport {

    @Override
    protected String doApply(ExecuteCommandEntity requestParameter,
                             DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("\n🎯 === 执行第 {} 步 ===", dynamicContext.getStep());
        log.info("\n📊 阶段1: 任务状态分析");

        String analysisPrompt = """
                原始用户需求:
                %s

                当前执行步数:
                第 %d 步 / 最多 %d 步

                历史执行记录:
                %s

                当前任务:
                %s

                你的职责:
                1. 判断当前轮最应该解决的核心问题。
                2. 明确后续执行重点，不要泛泛拆解。
                3. 如果当前信息已经足够直接回答用户，就标记为完成。

                运行约束:
                1. 当前链路没有显式 MCP / Tool 调用，知识检索由系统内置 RAG Advisor 自动完成。
                2. 不要虚构任何工具名、函数名、MCP 名称或 JSON 调用参数。
                3. 如果需要继续使用知识空间，只描述“继续基于当前 knowledgeSpaceId 检索并评估”。

                输出格式要求:
                任务状态分析: 用 2 行以内说明当前任务处于什么阶段
                执行历史评估: 用 2 行以内说明上一轮结果是否有效
                下一步策略: 用 3 行以内给出下一步执行重点
                完成度评估: 只能输出 0-100%%
                任务状态: 只能输出 CONTINUE 或 COMPLETED

                长度约束:
                1. 每个字段尽量单段输出，不要写长段落。
                2. 总输出控制在 220 个中文字符以内。
                3. 不要附加任何额外标题、解释或示例。
                """.formatted(
                requestParameter.getMessage(),
                dynamicContext.getStep(),
                dynamicContext.getMaxStep(),
                !dynamicContext.getExecutionHistory().isEmpty() ? dynamicContext.getExecutionHistory() : "[首次执行]",
                dynamicContext.getCurrentTask()
        );

        AiAgentClientFlowConfigVO flowConfig =
                dynamicContext.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.TASK_ANALYZER_CLIENT.getCode());
        ChatClient chatClient = getChatClientByClientId(flowConfig.getClientId());
        long stageStart = System.currentTimeMillis();
        String location = getClass().getSimpleName() + "#doApply";

        String analysisResult = executeStage(
                ExecutionStage.STEP1_ANALYZE,
                requestParameter,
                dynamicContext,
                () -> {
                    return callChatClientWithAudit(
                            requestParameter,
                            dynamicContext,
                            ExecutionStage.STEP1_ANALYZE,
                            "step1_analyze",
                            flowConfig.getClientId(),
                            location,
                            () -> chatClient
                                    .prompt(analysisPrompt)
                                    .advisors(a -> {
                                        a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, requestParameter.getSessionId())
                                                .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 256);
                                        if (StringUtils.hasText(requestParameter.getQaFilterExpression())) {
                                            a.param(QA_FILTER_EXPRESSION_KEY, requestParameter.getQaFilterExpression());
                                        }
                                    })
                                    .call()
                                    .chatResponse()
                    );
                },
                failure -> buildAnalysisFallback(failure)
        );
        recordStageMetric(requestParameter, dynamicContext, ExecutionStage.STEP1_ANALYZE, "step1_analyze", flowConfig.getClientId(), location, System.currentTimeMillis() - stageStart);

        parseAnalysisResult(dynamicContext, analysisResult, requestParameter.getSessionId());
        dynamicContext.setValue("analysisResult", analysisResult);

        if (analysisResult.contains("任务状态: COMPLETED") || analysisResult.contains("完成度评估: 100%")) {
            dynamicContext.setCompleted(true);
            log.info("✅ 任务分析显示已完成");
        }

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> get(
            ExecuteCommandEntity requestParameter,
            DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        if (dynamicContext.isCompleted() || dynamicContext.getStep() > dynamicContext.getMaxStep()) {
            return getBean("step4LogExecutionSummaryNode");
        }
        return getBean("step2PrecisionExecutorNode");
    }

    private void parseAnalysisResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                     String analysisResult,
                                     String sessionId) {
        int step = dynamicContext.getStep();
        log.info("\n📊 === 第 {} 步分析结果 ===", step);

        String[] lines = analysisResult.split("\n");
        String currentSection = "";
        StringBuilder sectionContent = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.contains("任务状态分析:")) {
                sendAnalysisSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "analysis_status";
                sectionContent = new StringBuilder();
                log.info("\n🎯 任务状态分析:");
                appendSectionLine(sectionContent, line);
                continue;
            }
            if (line.contains("执行历史评估:")) {
                sendAnalysisSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "analysis_history";
                sectionContent = new StringBuilder();
                log.info("\n📈 执行历史评估:");
                appendSectionLine(sectionContent, line);
                continue;
            }
            if (line.contains("下一步策略:")) {
                sendAnalysisSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "analysis_strategy";
                sectionContent = new StringBuilder();
                log.info("\n🚀 下一步策略:");
                appendSectionLine(sectionContent, line);
                continue;
            }
            if (line.contains("完成度评估:")) {
                sendAnalysisSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "analysis_progress";
                sectionContent = new StringBuilder();
                log.info("\n📊 {}", line);
                appendSectionLine(sectionContent, line);
                continue;
            }
            if (line.contains("任务状态:")) {
                sendAnalysisSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "analysis_task_status";
                sectionContent = new StringBuilder();
                log.info("\n🔄 {}", line);
                appendSectionLine(sectionContent, line);
                continue;
            }

            if (!currentSection.isEmpty()) {
                appendSectionLine(sectionContent, line);
                log.info("   {}", line);
            }
        }

        sendAnalysisSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
    }

    private void sendAnalysisSubResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                       String subType,
                                       String content,
                                       String sessionId) {
        if (!subType.isEmpty() && !content.isEmpty()) {
            AutoAgentExecuteResultEntity result = AutoAgentExecuteResultEntity.createAnalysisSubResult(
                    dynamicContext.getStep(), subType, content, sessionId);
            sendSseResult(dynamicContext, result);
        }
    }

    private void appendSectionLine(StringBuilder sectionContent, String line) {
        if (!sectionContent.isEmpty()) {
            sectionContent.append("\n");
        }
        sectionContent.append(line);
    }

    private String buildAnalysisFallback(ExecutionFailure failure) {
        return """
                任务状态分析: 分析阶段发生异常，已切换为降级分析并继续执行。
                执行历史评估: 当前保留已有上下文，后续阶段直接围绕用户问题产出可用结果。
                下一步策略: 跳过复杂规划，优先基于现有召回内容给出直接结论。
                完成度评估: 35%
                任务状态: CONTINUE
                降级原因: %s
                """.formatted(failure.getErrorCode().getMessage());
    }
}
