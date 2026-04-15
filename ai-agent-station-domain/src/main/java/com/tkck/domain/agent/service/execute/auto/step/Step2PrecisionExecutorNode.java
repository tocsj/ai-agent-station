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

/**
 * 精准执行节点
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/27 16:42
 */
@Slf4j
@Service
public class Step2PrecisionExecutorNode extends AbstractExecuteSupport{

    @Override
    protected String doApply(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("\n⚡ 阶段2: 精准任务执行");
        
        // 从动态上下文中获取分析结果
        String analysisResult = dynamicContext.getValue("analysisResult");
        if (analysisResult == null || analysisResult.trim().isEmpty()) {
            log.warn("⚠️ 分析结果为空，使用默认执行策略");
            analysisResult = "执行当前任务步骤";
        }
        
        final String finalAnalysisResult = analysisResult;
        String executionPrompt = String.format("""
                **用户原始需求:** %s
                
                **分析师策略:** %s
                
                **执行指令:** 你是一个精准任务执行器，需要根据用户需求和分析师策略，实际执行具体的任务。
                
                **执行要求:**
                1. 直接执行用户的具体需求（如搜索、检索、生成内容等）
                2. 如果需要搜索信息，请实际进行搜索和检索
                3. 如果需要生成计划、列表等，请直接生成完整内容
                4. 提供具体的执行结果，而不只是描述过程
                5. 确保执行结果能直接回答用户的问题
                
                **输出格式:**
                执行目标: [明确的执行目标]
                执行过程: [实际执行的步骤和调用的工具]
                执行结果: [具体的执行成果和获得的信息/内容]
                质量检查: [对执行结果的质量评估]
                """, requestParameter.getMessage(), finalAnalysisResult);

        // 获取对话客户端
        executionPrompt += """

                **当前运行约束:**
                1. 当前知识检索由 RAG Advisor 自动注入，请直接基于召回到的简历内容执行评估。
                2. 不要等待、调用或引用不存在的 knowledge_space_retrieve 等工具。
                3. 如果当前召回内容不足，请直接指出“当前召回内容不足”，不要编造外部工具返回结果。
                """;

        final String finalExecutionPrompt = executionPrompt;
        AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO = dynamicContext.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.PRECISION_EXECUTOR_CLIENT.getCode());
        ChatClient chatClient = getChatClientByClientId(aiAgentClientFlowConfigVO.getClientId());

        String executionResult = executeStage(
                ExecutionStage.STEP2_EXECUTE,
                requestParameter,
                dynamicContext,
                () -> chatClient
                        .prompt(finalExecutionPrompt)
                        .advisors(a -> {
                            a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, requestParameter.getSessionId())
                                    .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1024);
                            if (StringUtils.hasText(requestParameter.getQaFilterExpression())) {
                                a.param(QA_FILTER_EXPRESSION_KEY, requestParameter.getQaFilterExpression());
                            }
                        })
                        .call().content(),
                failure -> buildExecutionFallback(requestParameter, finalAnalysisResult, failure)
        );

        parseExecutionResult(dynamicContext, executionResult, requestParameter.getSessionId());
        
        // 将执行结果保存到动态上下文中，供下一步使用
        dynamicContext.setValue("executionResult", executionResult);
        
        // 更新执行历史
        String stepSummary = String.format("""
                === 第 %d 步执行记录 ===
                【分析阶段】%s
                【执行阶段】%s
                """, dynamicContext.getStep(), finalAnalysisResult, executionResult);
        
        dynamicContext.getExecutionHistory().append(stepSummary);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> get(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return getBean("step3QualitySupervisorNode");
    }
    
    /**
     * 解析执行结果
     */
    private void parseExecutionResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext, String executionResult, String sessionId) {
        int step = dynamicContext.getStep();
        log.info("\n⚡ === 第 {} 步执行结果 ===", step);
        
        String[] lines = executionResult.split("\n");
        String currentSection = "";
        StringBuilder sectionContent = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (line.contains("执行目标:")) {
                // 发送上一个section的内容
                sendExecutionSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "execution_target";
                sectionContent = new StringBuilder();
                log.info("\n🎯 执行目标:");
                continue;
            } else if (line.contains("执行过程:")) {
                // 发送上一个section的内容
                sendExecutionSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "execution_process";
                sectionContent = new StringBuilder();
                log.info("\n🔧 执行过程:");
                continue;
            } else if (line.contains("执行结果:")) {
                // 发送上一个section的内容
                sendExecutionSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "execution_result";
                sectionContent = new StringBuilder();
                log.info("\n📈 执行结果:");
                continue;
            } else if (line.contains("质量检查:")) {
                // 发送上一个section的内容
                sendExecutionSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "execution_quality";
                sectionContent = new StringBuilder();
                log.info("\n🔍 质量检查:");
                continue;
            }
            
            // 收集当前section的内容
            if (!currentSection.isEmpty()) {
                sectionContent.append(line).append("\n");
                switch (currentSection) {
                    case "execution_target":
                        log.info("   🎯 {}", line);
                        break;
                    case "execution_process":
                        log.info("   ⚙️ {}", line);
                        break;
                    case "execution_result":
                        log.info("   📊 {}", line);
                        break;
                    case "execution_quality":
                        log.info("   ✅ {}", line);
                        break;
                    default:
                        log.info("   📝 {}", line);
                        break;
                }
            }
        }
        
        // 发送最后一个section的内容
        sendExecutionSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
    }
    
    /**
     * 发送执行阶段细分结果到流式输出
     */
    private void sendExecutionSubResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext, 
                                       String subType, String content, String sessionId) {
        // 抽取的通用判断逻辑
        if (!subType.isEmpty() && !content.isEmpty()) {
            AutoAgentExecuteResultEntity result = AutoAgentExecuteResultEntity.createExecutionSubResult(
                    dynamicContext.getStep(), subType, content, sessionId);
            sendSseResult(dynamicContext, result);
        }
    }
    
    private String buildExecutionFallback(ExecuteCommandEntity requestParameter,
                                          String analysisResult,
                                          ExecutionFailure failure) {
        return """
                执行目标:
                围绕当前用户问题生成可直接消费的结果。
                执行过程:
                核心执行阶段出现异常，已切换为降级执行，仅保留分析结果和已有上下文。
                执行结果:
                当前问题是：%s
                当前分析是：%s
                请以后续总结阶段输出保守结论，并标记结果为降级版本。
                质量检查:
                该结果未经完整执行链验证，建议稍后重试。
                降级原因:
                %s
                """.formatted(requestParameter.getMessage(), analysisResult, failure.getErrorCode().getMessage());
    }
}
