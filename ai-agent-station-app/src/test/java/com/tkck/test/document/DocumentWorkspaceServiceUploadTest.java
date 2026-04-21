package com.tkck.test.document;

import com.tkck.app.document.DocumentWorkspaceServiceImpl;
import com.tkck.domain.document.adapter.repository.IDocumentWorkspaceRepository;
import com.tkck.domain.document.model.entity.DocumentFileEntity;
import com.tkck.domain.document.service.IQueryRewriteService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DocumentWorkspaceServiceUploadTest {

    @Test
    public void should_upload_document_and_write_vector_chunks() throws Exception {
        IDocumentWorkspaceRepository repository = mock(IDocumentWorkspaceRepository.class);
        VectorStore documentVectorStore = mock(VectorStore.class);
        TokenTextSplitter tokenTextSplitter = mock(TokenTextSplitter.class);
        IQueryRewriteService queryRewriteService = mock(IQueryRewriteService.class);

        DocumentWorkspaceServiceImpl service = new DocumentWorkspaceServiceImpl(repository, documentVectorStore, tokenTextSplitter, queryRewriteService) {
            @Override
            protected String readDocumentText(byte[] bytes, String fileName) {
                return "鏋舵瀯鍒嗗眰\n鏈嶅姟鎷嗗垎\n缂撳瓨璁捐";
            }
        };

        when(repository.existsWorkspace("dws_001")).thenReturn(true);
        when(tokenTextSplitter.apply(any())).thenReturn(List.of(
                new Document("鏋舵瀯鍒嗗眰"),
                new Document("鏈嶅姟鎷嗗垎")
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
        verify(repository, times(1)).saveDocument(any(DocumentFileEntity.class));
        verify(repository, times(2)).saveChunk(anyString(), anyString(), anyString(), anyInt(), anyString(), anyString());
        verify(repository, times(1)).updateDocumentParseResult(anyString(), eq("COMPLETED"), eq(2), eq("COMPLETED"));
    }
}
