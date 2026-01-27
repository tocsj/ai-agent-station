package com.tkck.domain.agent.service.armory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import com.tkck.domain.agent.model.entity.ArmoryCommandEntity;
import com.tkck.domain.agent.model.valobj.enums.AiAgentEnumVO;
import com.tkck.domain.agent.model.valobj.AiClientSystemPromptVO;
import com.tkck.domain.agent.model.valobj.AiClientVO;
import com.tkck.domain.agent.service.armory.factory.DefaultArmoryStrategyFactory;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiClientNode extends AbstractArmorySupport{
    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 构建节点，客户端{}", JSON.toJSONString(requestParameter));
        List<AiClientVO> aiClientList =  dynamicContext.getValue(dataName());
        if(aiClientList == null || aiClientList.isEmpty()){
            log.warn("没有需要被初始化的 ai client");
            return router(requestParameter, dynamicContext);
        }
        Map<String, AiClientSystemPromptVO> systemPromptMap = dynamicContext.getValue(AiAgentEnumVO.AI_CLIENT_SYSTEM_PROMPT.getDataName());

        for(AiClientVO aiClientVO : aiClientList){
            // 1. 预设话术
            StringBuilder defaultSystem = new StringBuilder("Ai 智能体 \r\n");
            List<String> promptIdList = aiClientVO.getPromptIdList();
            for (String promptId : promptIdList) {
                AiClientSystemPromptVO aiClientSystemPromptVO = systemPromptMap.get(promptId);
                defaultSystem.append(aiClientSystemPromptVO.getPromptContent());
            }
            //2.对话模型
            OpenAiChatModel chatModel = getBean(aiClientVO.getModelBeanName());
            //3.MCP服务
            List<McpSyncClient> mcpSyncClients = new ArrayList<>();
            List<String> mcpBeanNameList = aiClientVO.getMcpBeanNameList();
            for(String mcpBeanName : mcpBeanNameList){
                McpSyncClient mcpSyncClient = getBean(mcpBeanName);
                mcpSyncClients.add(mcpSyncClient);
            }
            //4.顾问角色
            List<Advisor> advisorList = new ArrayList<>();
            List<String> advisorBeanNameList = aiClientVO.getAdvisorBeanNameList();
            for(String advisorBeanName : advisorBeanNameList){
                Advisor advisor = getBean(advisorBeanName);
                advisorList.add(advisor);
            }
            Advisor[] advisorArray = advisorList.toArray(new Advisor[]{});

            // 5. 构建对话客户端
            // 5. 构建对话客户端
            ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultSystem(defaultSystem.toString())
                    .defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClients.toArray(new McpSyncClient[]{})))
                    .defaultAdvisors(advisorArray)
                    .build();
            registerBean(beanName(aiClientVO.getClientId()), ChatClient.class, chatClient);

        }
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }
    @Override
    protected String beanName(String id) {
        return AiAgentEnumVO.AI_CLIENT.getBeanName(id);
    }

    @Override
    protected String dataName() {
        return AiAgentEnumVO.AI_CLIENT.getDataName();
    }

}
