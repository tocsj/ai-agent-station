package com.tkck.test.document;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tkck.app.document.DocumentWorkspaceServiceImpl;
import com.tkck.domain.document.adapter.repository.IDocumentWorkspaceRepository;
import com.tkck.domain.document.model.entity.DocumentQueryRewriteCommandEntity;
import com.tkck.domain.document.model.entity.DocumentQueryRewriteResultEntity;
import com.tkck.domain.document.model.entity.DocumentTaskResultEntity;
import com.tkck.domain.document.service.IQueryRewriteService;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DocumentWorkspaceLoggingTest {

    @Test
    public void shouldLogDocumentTaskLifecycleForAsk() {
        IDocumentWorkspaceRepository repository = mock(IDocumentWorkspaceRepository.class);
        VectorStore documentVectorStore = mock(VectorStore.class);
        TokenTextSplitter tokenTextSplitter = mock(TokenTextSplitter.class);
        IQueryRewriteService queryRewriteService = mock(IQueryRewriteService.class);

        when(repository.existsWorkspace("dws_001")).thenReturn(true);
        when(repository.queryActiveDocumentIds("dws_001")).thenReturn(Set.of("doc_001", "doc_002"));
        when(queryRewriteService.rewrite(any(DocumentQueryRewriteCommandEntity.class))).thenReturn(
                DocumentQueryRewriteResultEntity.builder().rewrittenQuery("rewritten ask query").build()
        );

        when(documentVectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("绗竴浠芥枃妗ｆ彁鍒扮郴缁熼噰鐢ㄥ垎灞傛灦鏋勩€?", Map.of("spaceId", "dws_001", "docId", "doc_001")),
                new Document("绗簩浠芥枃妗ｆ彁鍒扮紦瀛樻斁鍦ㄥ簲鐢ㄥ眰涓庢暟鎹眰涔嬮棿銆?", Map.of("spaceId", "dws_001", "docId", "doc_002"))
        ));

        DocumentWorkspaceServiceImpl service = new DocumentWorkspaceServiceImpl(repository, documentVectorStore, tokenTextSplitter, queryRewriteService) {
            @Override
            protected String generateAnswer(String traceId,
                                            String taskType,
                                            String workspaceId,
                                            String sessionId,
                                            String prompt,
                                            List<Document> documents) {
                return "鍩轰簬妫€绱㈢粨鏋滅殑鍥炵瓟";
            }
        };

        Logger logger = (Logger) LoggerFactory.getLogger(DocumentWorkspaceServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            DocumentTaskResultEntity result = service.ask("dws_001", null, "杩欏绯荤粺鐨勬牳蹇冩灦鏋勬槸浠€涔堬紵");

            Assert.assertEquals("鍩轰簬妫€绱㈢粨鏋滅殑鍥炵瓟", result.getAnswer());
            Assert.assertTrue(listAppender.list.stream().anyMatch(event ->
                    event.getLevel() == Level.INFO
                            && event.getFormattedMessage().contains("document task start")
                            && event.getFormattedMessage().contains("taskType=ask")));
            Assert.assertTrue(listAppender.list.stream().anyMatch(event ->
                    event.getLevel() == Level.INFO
                            && event.getFormattedMessage().contains("document retrieval completed")
                            && event.getFormattedMessage().contains("retrievedChunks=2")));
            Assert.assertTrue(listAppender.list.stream().anyMatch(event ->
                    event.getLevel() == Level.INFO
                            && event.getFormattedMessage().contains("document task completed")
                            && event.getFormattedMessage().contains("taskType=ask")));
        } finally {
            logger.detachAppender(listAppender);
        }
    }
}
