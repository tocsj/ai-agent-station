package com.tkck.app.content.workflow;

import com.tkck.app.content.ContentChatClientFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

@Slf4j
public abstract class AbstractContentWorkflowNode implements ContentWorkflowNode {

    @Resource
    protected ContentChatClientFactory contentChatClientFactory;

    protected String generate(String prompt) {
        if (contentChatClientFactory == null) {
            return prompt;
        }
        ChatClient chatClient = contentChatClientFactory.getClient(clientId());
        String content = chatClient.prompt(prompt).call().content();
        log.info("内容节点生成完成, node={}, clientId={}, promptLength={}, resultLength={}",
                getClass().getSimpleName(),
                clientId(),
                prompt == null ? 0 : prompt.length(),
                content == null ? 0 : content.length());
        return content == null ? "" : content;
    }

    protected String clientId() {
        return "5301";
    }

    protected String safe(String value) {
        return value == null ? "" : value;
    }
}
