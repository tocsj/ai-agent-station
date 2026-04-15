package com.tkck.test.document;

import com.tkck.app.document.DocumentWorkspaceServiceImpl;
import com.tkck.domain.document.model.entity.DocumentFileEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DocumentWorkspaceServiceUploadTest {

    @Test
    public void should_upload_document_and_write_vector_chunks() throws Exception {
        JdbcTemplate mysqlJdbcTemplate = mock(JdbcTemplate.class);
        VectorStore documentVectorStore = mock(VectorStore.class);
        TokenTextSplitter tokenTextSplitter = mock(TokenTextSplitter.class);

        DocumentWorkspaceServiceImpl service = new DocumentWorkspaceServiceImpl(mysqlJdbcTemplate, documentVectorStore, tokenTextSplitter) {
            @Override
            protected String readDocumentText(byte[] bytes, String fileName) {
                return "架构分层\n服务拆分\n缓存设计";
            }
        };

        when(mysqlJdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("dws_001"))).thenReturn(1);
        when(tokenTextSplitter.apply(anyList())).thenReturn(List.of(
                new Document("架构分层"),
                new Document("服务拆分")
        ));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "architecture.md",
                "text/markdown",
                "# Architecture".getBytes()
        );

        DocumentFileEntity result = service.upload("dws_001", file);

        Assert.assertEquals("dws_001", result.getWorkspaceId());
        Assert.assertEquals("architecture.md", result.getFileName());
        Assert.assertEquals("md", result.getFileType());
        Assert.assertEquals(Integer.valueOf(2), result.getChunkCount());
        verify(documentVectorStore, times(1)).accept(argThat(list -> list != null && list.size() == 2));
        verify(mysqlJdbcTemplate, atLeastOnce()).update(anyString(), any(Object[].class));
    }
}
