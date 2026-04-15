package com.tkck.test.document;

import com.tkck.app.document.DocumentWorkspaceServiceImpl;
import com.tkck.domain.document.model.entity.DocumentWorkspaceEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DocumentWorkspaceListServiceTest {

    @Test
    public void shouldReturnWorkspaceHistoryOrderedByUpdateTime() {
        JdbcTemplate mysqlJdbcTemplate = mock(JdbcTemplate.class);
        VectorStore documentVectorStore = mock(VectorStore.class);
        TokenTextSplitter tokenTextSplitter = mock(TokenTextSplitter.class);

        when(mysqlJdbcTemplate.queryForList(contains("FROM ai_knowledge_space"))).thenReturn(List.of(
                Map.of(
                        "space_id", "dws_new",
                        "space_name", "new-space",
                        "description", "latest",
                        "status", 1,
                        "document_count", 2,
                        "update_time", Timestamp.valueOf(LocalDateTime.of(2026, 4, 15, 10, 0, 0))
                ),
                Map.of(
                        "space_id", "dws_old",
                        "space_name", "old-space",
                        "description", "older",
                        "status", 1,
                        "document_count", 1,
                        "update_time", Timestamp.valueOf(LocalDateTime.of(2026, 4, 14, 10, 0, 0))
                )
        ));

        DocumentWorkspaceServiceImpl service = new DocumentWorkspaceServiceImpl(mysqlJdbcTemplate, documentVectorStore, tokenTextSplitter);

        List<DocumentWorkspaceEntity> result = service.listWorkspaces();

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("dws_new", result.get(0).getWorkspaceId());
        Assert.assertEquals(Integer.valueOf(2), result.get(0).getDocumentCount());
        Assert.assertNotNull(result.get(0).getUpdateTime());
    }
}
