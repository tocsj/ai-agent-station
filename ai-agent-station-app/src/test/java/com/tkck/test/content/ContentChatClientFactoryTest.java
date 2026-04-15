package com.tkck.test.content;

import com.tkck.app.content.ContentChatClientFactory;
import com.tkck.domain.agent.model.valobj.enums.AiAgentEnumVO;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationContext;

public class ContentChatClientFactoryTest {

    @Test
    public void should_use_dedicated_content_clients_instead_of_shared_client_5201() {
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        ChatClient highQualityClient = Mockito.mock(ChatClient.class);
        ChatClient lightweightClient = Mockito.mock(ChatClient.class);
        Mockito.when(applicationContext.getBean(
                        AiAgentEnumVO.AI_CLIENT.getBeanName("5301"),
                        ChatClient.class))
                .thenReturn(highQualityClient);
        Mockito.when(applicationContext.getBean(
                        AiAgentEnumVO.AI_CLIENT.getBeanName("5302"),
                        ChatClient.class))
                .thenReturn(lightweightClient);

        ContentChatClientFactory factory = new ContentChatClientFactory(applicationContext);
        factory.getClient("5301");
        factory.getClient("5302");

        Mockito.verify(applicationContext).getBean(
                AiAgentEnumVO.AI_CLIENT.getBeanName("5301"),
                ChatClient.class);
        Mockito.verify(applicationContext).getBean(
                AiAgentEnumVO.AI_CLIENT.getBeanName("5302"),
                ChatClient.class);
        Mockito.verify(applicationContext, Mockito.never()).getBean(
                AiAgentEnumVO.AI_CLIENT.getBeanName("5201"));
    }
}
