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
import static org.mockito.Mockito.when;

public class DocumentWorkspaceOptionalTaskServiceTest {

    @Test
    public void shouldBuildStructuredSummaryResult() {
        DocumentWorkspaceServiceImpl service = newService();

        DocumentTaskResultEntity result = service.summary("dws_001", null, "summary-mode");

        Assert.assertEquals("mock-answer", result.getAnswer());
        Assert.assertEquals("请基于当前文档空间生成summary-mode", result.getRewrittenQuery());
        Assert.assertEquals(1, result.getRetrievedChunks().size());
    }

    @Test
    public void shouldBuildFollowupResult() {
        DocumentWorkspaceServiceImpl service = newService();

        DocumentTaskResultEntity result = service.followup("dws_001", null, "review");

        Assert.assertEquals("mock-answer", result.getAnswer());
        Assert.assertEquals("请从review视角生成文档追问", result.getRewrittenQuery());
        Assert.assertEquals(1, result.getRetrievedChunks().size());
    }

    @Test
    public void shouldBuildQuizResult() {
        DocumentWorkspaceServiceImpl service = newService();

        DocumentTaskResultEntity result = service.quiz("dws_001", null, 3, "short-answer");

        Assert.assertEquals("mock-answer", result.getAnswer());
        Assert.assertEquals("请基于当前文档生成3道short-answer", result.getRewrittenQuery());
        Assert.assertEquals(1, result.getRetrievedChunks().size());
    }

    private DocumentWorkspaceServiceImpl newService() {
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
                Map.of("doc_id", "doc_001")
        ));

        when(documentVectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("matched workspace chunk", Map.of("spaceId", "dws_001", "docId", "doc_001", "fileName", "guide.md", "chunkIndex", "0"))
        ));

        return new DocumentWorkspaceServiceImpl(mysqlJdbcTemplate, documentVectorStore, tokenTextSplitter) {
            @Override
            protected String generateAnswer(String traceId,
                                            String taskType,
                                            String workspaceId,
                                            String sessionId,
                                            String prompt,
                                            List<Document> documents) {
                return "mock-answer";
            }
        };
    }
}
