package com.tkck.test.document;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tkck.app.document.DocumentWorkspaceServiceImpl;
import com.tkck.domain.document.model.entity.DocumentTaskResultEntity;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DocumentWorkspaceLoggingTest {

    @Test
    public void shouldLogDocumentTaskLifecycleForAsk() {
        JdbcTemplate mysqlJdbcTemplate = mock(JdbcTemplate.class);
        VectorStore documentVectorStore = mock(VectorStore.class);
        TokenTextSplitter tokenTextSplitter = mock(TokenTextSplitter.class);

        when(mysqlJdbcTemplate.queryForObject(
                contains("SELECT COUNT(1) FROM ai_knowledge_space"),
                eq(Integer.class),
                eq("dws_001"))).thenReturn(1);
        when(mysqlJdbcTemplate.queryForList(
                contains("SELECT doc_id FROM ai_knowledge_document"),
                eq("dws_001"))).thenReturn(List.of(
                Map.of("doc_id", "doc_001"),
                Map.of("doc_id", "doc_002")
        ));

        when(documentVectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("第一份文档提到系统采用分层架构。", Map.of("spaceId", "dws_001", "docId", "doc_001")),
                new Document("第二份文档提到缓存放在应用层与数据层之间。", Map.of("spaceId", "dws_001", "docId", "doc_002"))
        ));

        DocumentWorkspaceServiceImpl service = new DocumentWorkspaceServiceImpl(mysqlJdbcTemplate, documentVectorStore, tokenTextSplitter) {
            @Override
            protected String generateAnswer(String prompt, List<Document> documents) {
                return "基于检索结果的回答";
            }
        };

        Logger logger = (Logger) LoggerFactory.getLogger(DocumentWorkspaceServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            DocumentTaskResultEntity result = service.ask("dws_001", null, "这套系统的核心架构是什么？");

            Assert.assertEquals("基于检索结果的回答", result.getAnswer());
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
