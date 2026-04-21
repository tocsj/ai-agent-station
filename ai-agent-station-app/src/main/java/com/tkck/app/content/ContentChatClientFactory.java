package com.tkck.app.content;

import com.tkck.domain.agent.model.valobj.enums.AiAgentEnumVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ContentChatClientFactory {

    private final ApplicationContext applicationContext;
    private final Map<String, ChatClient> chatClientCache = new ConcurrentHashMap<>();

    public ContentChatClientFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ChatClient getClient(String clientId) {
        return chatClientCache.computeIfAbsent(clientId, this::loadClient);
    }

    private ChatClient loadClient(String clientId) {
        String beanName = AiAgentEnumVO.AI_CLIENT.getBeanName(clientId);
        ChatClient chatClient = applicationContext.getBean(beanName, ChatClient.class);
        log.info("内容工作流专用客户端初始化完成, clientId={}, beanName={}", clientId, beanName);
        return chatClient;
    }
}
