package com.tkck.test.document;

import com.tkck.app.document.DocumentWorkspaceServiceImpl;
import com.tkck.domain.agent.model.valobj.enums.AiAgentEnumVO;
import com.tkck.domain.audit.model.entity.AuditLlmCallMetricEntity;
import com.tkck.domain.audit.service.IAuditMonitoringService;
import com.tkck.domain.document.adapter.repository.IDocumentWorkspaceRepository;
import com.tkck.domain.document.service.IQueryRewriteService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

public class DocumentWorkspaceGenerateAnswerAuditTest {

    @Test
    public void should_use_dedicated_document_client_and_record_model_2008() {
        IDocumentWorkspaceRepository repository = Mockito.mock(IDocumentWorkspaceRepository.class);
        VectorStore documentVectorStore = Mockito.mock(VectorStore.class);
        TokenTextSplitter tokenTextSplitter = Mockito.mock(TokenTextSplitter.class);
        IQueryRewriteService queryRewriteService = Mockito.mock(IQueryRewriteService.class);
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        IAuditMonitoringService auditMonitoringService = Mockito.mock(IAuditMonitoringService.class);

        ChatClient chatClient = Mockito.mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = Mockito.mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = Mockito.mock(CallResponseSpec.class);
        ChatResponse chatResponse = Mockito.mock(ChatResponse.class);
        Generation generation = Mockito.mock(Generation.class);
        ChatResponseMetadata metadata = Mockito.mock(ChatResponseMetadata.class);
        Usage usage = Mockito.mock(Usage.class);

        Mockito.when(applicationContext.getBean(AiAgentEnumVO.AI_CLIENT.getBeanName("5401")))
                .thenReturn(chatClient);
        Mockito.when(chatClient.prompt(Mockito.anyString())).thenReturn(requestSpec);
        Mockito.when(requestSpec.call()).thenReturn(responseSpec);
        Mockito.when(responseSpec.chatResponse()).thenReturn(chatResponse);
        Mockito.when(chatResponse.getResult()).thenReturn(generation);
        Mockito.when(generation.getOutput()).thenReturn(new AssistantMessage("document-answer"));
        Mockito.when(chatResponse.getMetadata()).thenReturn(metadata);
        Mockito.when(metadata.getUsage()).thenReturn(usage);
        Mockito.when(usage.getPromptTokens()).thenReturn(11);
        Mockito.when(usage.getCompletionTokens()).thenReturn(22);
        Mockito.when(usage.getTotalTokens()).thenReturn(33);

        TestableDocumentWorkspaceService service = new TestableDocumentWorkspaceService(repository, documentVectorStore, tokenTextSplitter, queryRewriteService);
        ReflectionTestUtils.setField(service, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(service, "auditMonitoringService", auditMonitoringService);

        String answer = service.invokeGenerateAnswer(
                "trace_x",
                "ask",
                "dws_001",
                "session_001",
                "prompt",
                List.of(new Document("chunk-1")));

        Assert.assertEquals("document-answer", answer);
        Mockito.verify(applicationContext).getBean(AiAgentEnumVO.AI_CLIENT.getBeanName("5401"));

        ArgumentCaptor<AuditLlmCallMetricEntity> captor = ArgumentCaptor.forClass(AuditLlmCallMetricEntity.class);
        Mockito.verify(auditMonitoringService).recordLlmCall(captor.capture());
        Assert.assertEquals("5401", captor.getValue().getClientId());
        Assert.assertEquals("2008", captor.getValue().getModelCode());
    }

    private static class TestableDocumentWorkspaceService extends DocumentWorkspaceServiceImpl {

        protected TestableDocumentWorkspaceService(IDocumentWorkspaceRepository repository,
                                                   VectorStore documentVectorStore,
                                                   TokenTextSplitter tokenTextSplitter,
                                                   IQueryRewriteService queryRewriteService) {
            super(repository, documentVectorStore, tokenTextSplitter, queryRewriteService);
        }

        public String invokeGenerateAnswer(String traceId,
                                           String taskType,
                                           String workspaceId,
                                           String sessionId,
                                           String prompt,
                                           List<Document> documents) {
            return generateAnswer(traceId, taskType, workspaceId, sessionId, prompt, documents);
        }
    }
}
