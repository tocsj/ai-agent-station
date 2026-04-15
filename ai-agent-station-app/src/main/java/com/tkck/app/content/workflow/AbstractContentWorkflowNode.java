package com.tkck.app.content.workflow;

import com.tkck.domain.agent.model.valobj.enums.AiAgentEnumVO;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationContext;

public abstract class AbstractContentWorkflowNode implements ContentWorkflowNode {

    @Resource
    protected ApplicationContext applicationContext;

    protected String generate(String prompt) {
        if (applicationContext == null) {
            return prompt;
        }
        ChatClient chatClient = (ChatClient) applicationContext.getBean(AiAgentEnumVO.AI_CLIENT.getBeanName("5201"));
        String content = chatClient.prompt(prompt).call().content();
        return content == null ? "" : content;
    }

    protected String safe(String value) {
        return value == null ? "" : value;
    }
}
