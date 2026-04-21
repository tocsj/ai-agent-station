package com.tkck.test.document;

import com.tkck.app.document.DocumentWorkspaceServiceImpl;
import com.tkck.domain.document.adapter.repository.IDocumentWorkspaceRepository;
import com.tkck.domain.document.model.entity.DocumentQueryRewriteCommandEntity;
import com.tkck.domain.document.model.entity.DocumentQueryRewriteResultEntity;
import com.tkck.domain.document.model.entity.DocumentTaskResultEntity;
import com.tkck.domain.document.service.IQueryRewriteService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DocumentWorkspaceAskServiceTest {

    @Test
    public void shouldBuildWorkspaceScopedAnswerWithRetrievedChunks() {
        IDocumentWorkspaceRepository repository = mock(IDocumentWorkspaceRepository.class);
        VectorStore documentVectorStore = mock(VectorStore.class);
        TokenTextSplitter tokenTextSplitter = mock(TokenTextSplitter.class);
        IQueryRewriteService queryRewriteService = mock(IQueryRewriteService.class);

        DocumentWorkspaceServiceImpl service = new DocumentWorkspaceServiceImpl(
                repository,
                documentVectorStore,
                tokenTextSplitter,
                queryRewriteService
        ) {
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

        when(repository.existsWorkspace("dws_001")).thenReturn(true);
        when(repository.queryActiveDocumentIds("dws_001")).thenReturn(Set.of("doc_001", "doc_002"));
        when(queryRewriteService.rewrite(any(DocumentQueryRewriteCommandEntity.class))).thenReturn(
                DocumentQueryRewriteResultEntity.builder()
                        .originalQuestion("what is the core architecture?")
                        .rewrittenQuery("java backend core architecture design")
                        .build()
        );
        when(documentVectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("doc-1 matched chunk", Map.of("spaceId", "dws_001", "docId", "doc_001")),
                new Document("foreign workspace chunk", Map.of("spaceId", "dws_other", "docId", "doc_other")),
                new Document("stale metadata chunk", Map.of("spaceId", "dws_001", "docId", "doc_removed")),
                new Document("doc-2 matched chunk", Map.of("spaceId", "dws_001", "docId", "doc_002", "fileName", "a.txt", "chunkIndex", "2"))
        ));

        DocumentTaskResultEntity result = service.ask("dws_001", null, "what is the core architecture?");

        Assert.assertEquals("based-on-retrieval", result.getAnswer());
        Assert.assertEquals("java backend core architecture design", result.getRewrittenQuery());
        Assert.assertEquals("workspace:dws_001,vectorTable=document_vector_store", result.getRetrievalScope());
        Assert.assertEquals(2, result.getRetrievedChunks().size());
        Assert.assertEquals(2, result.getRetrievedChunkDetails().size());
        Assert.assertTrue(result.getFinalContext().contains("doc-1 matched chunk"));
        Assert.assertTrue(result.getFinalContext().contains("doc-2 matched chunk"));
        Assert.assertFalse(result.getFinalContext().contains("foreign workspace chunk"));
        Assert.assertFalse(result.getFinalContext().contains("stale metadata chunk"));
        Assert.assertEquals("a.txt", result.getRetrievedChunkDetails().get(1).getFileName());
        Assert.assertEquals("2", result.getRetrievedChunkDetails().get(1).getChunkIndex());

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(documentVectorStore).similaritySearch(requestCaptor.capture());
        Assert.assertEquals("java backend core architecture design", requestCaptor.getValue().getQuery());
    }
}
