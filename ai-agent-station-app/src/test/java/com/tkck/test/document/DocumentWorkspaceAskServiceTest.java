package com.tkck.test.document;

import com.tkck.app.document.DocumentWorkspaceServiceImpl;
import com.tkck.domain.document.model.entity.DocumentTaskResultEntity;
import org.junit.Assert;
import org.junit.Test;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DocumentWorkspaceAskServiceTest {

    @Test
    public void shouldBuildWorkspaceScopedAnswerWithRetrievedChunks() {
        JdbcTemplate mysqlJdbcTemplate = mock(JdbcTemplate.class);
        VectorStore documentVectorStore = mock(VectorStore.class);
        TokenTextSplitter tokenTextSplitter = mock(TokenTextSplitter.class);

        DocumentWorkspaceServiceImpl service = new DocumentWorkspaceServiceImpl(mysqlJdbcTemplate, documentVectorStore, tokenTextSplitter) {
            @Override
            protected String generateAnswer(String traceId,
                                            String taskType,
                                            String workspaceId,
                                            String sessionId,
                                            String prompt,
                                            List<Document> documents) {
                return "based-on-retrieval";
            }
        };

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
                new Document("doc-1 matched chunk", Map.of("spaceId", "dws_001", "docId", "doc_001")),
                new Document("foreign workspace chunk", Map.of("spaceId", "dws_other", "docId", "doc_other")),
                new Document("stale metadata chunk", Map.of("spaceId", "dws_001", "docId", "doc_removed")),
                new Document("doc-2 matched chunk", Map.of("spaceId", "dws_001", "docId", "doc_002", "fileName", "a.txt", "chunkIndex", "2"))
        ));

        DocumentTaskResultEntity result = service.ask("dws_001", null, "what is the core architecture?");

        Assert.assertEquals("based-on-retrieval", result.getAnswer());
        Assert.assertEquals("what is the core architecture?", result.getRewrittenQuery());
        Assert.assertEquals("workspace:dws_001,vectorTable=document_vector_store", result.getRetrievalScope());
        Assert.assertEquals(2, result.getRetrievedChunks().size());
        Assert.assertEquals(2, result.getRetrievedChunkDetails().size());
        Assert.assertTrue(result.getFinalContext().contains("doc-1 matched chunk"));
        Assert.assertTrue(result.getFinalContext().contains("doc-2 matched chunk"));
        Assert.assertFalse(result.getFinalContext().contains("foreign workspace chunk"));
        Assert.assertFalse(result.getFinalContext().contains("stale metadata chunk"));
        Assert.assertEquals("a.txt", result.getRetrievedChunkDetails().get(1).getFileName());
        Assert.assertEquals("2", result.getRetrievedChunkDetails().get(1).getChunkIndex());
        verify(documentVectorStore).similaritySearch(any(SearchRequest.class));
    }
}
