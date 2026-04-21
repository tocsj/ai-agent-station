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

public class DocumentWorkspaceOptionalTaskServiceTest {

    @Test
    public void shouldBuildStructuredSummaryResult() {
        IQueryRewriteService queryRewriteService = mock(IQueryRewriteService.class);
        when(queryRewriteService.rewrite(any(DocumentQueryRewriteCommandEntity.class))).thenReturn(
                DocumentQueryRewriteResultEntity.builder()
                        .rewrittenQuery("summary rewritten query")
                        .build()
        );
        DocumentWorkspaceServiceImpl service = newService(queryRewriteService);

        DocumentTaskResultEntity result = service.summary("dws_001", null, "summary-mode");

        Assert.assertEquals("mock-answer", result.getAnswer());
        Assert.assertEquals("summary rewritten query", result.getRewrittenQuery());
        Assert.assertEquals(1, result.getRetrievedChunks().size());

        ArgumentCaptor<DocumentQueryRewriteCommandEntity> captor = ArgumentCaptor.forClass(DocumentQueryRewriteCommandEntity.class);
        verify(queryRewriteService).rewrite(captor.capture());
        Assert.assertEquals("summary", captor.getValue().getTaskType());
    }

    @Test
    public void shouldBuildFollowupResult() {
        IQueryRewriteService queryRewriteService = mock(IQueryRewriteService.class);
        when(queryRewriteService.rewrite(any(DocumentQueryRewriteCommandEntity.class))).thenReturn(
                DocumentQueryRewriteResultEntity.builder()
                        .rewrittenQuery("followup rewritten query")
                        .build()
        );
        DocumentWorkspaceServiceImpl service = newService(queryRewriteService);

        DocumentTaskResultEntity result = service.followup("dws_001", null, "review");

        Assert.assertEquals("mock-answer", result.getAnswer());
        Assert.assertEquals("followup rewritten query", result.getRewrittenQuery());
        Assert.assertEquals(1, result.getRetrievedChunks().size());

        ArgumentCaptor<DocumentQueryRewriteCommandEntity> captor = ArgumentCaptor.forClass(DocumentQueryRewriteCommandEntity.class);
        verify(queryRewriteService).rewrite(captor.capture());
        Assert.assertEquals("followup", captor.getValue().getTaskType());
    }

    @Test
    public void shouldBuildQuizResult() {
        IQueryRewriteService queryRewriteService = mock(IQueryRewriteService.class);
        when(queryRewriteService.rewrite(any(DocumentQueryRewriteCommandEntity.class))).thenReturn(
                DocumentQueryRewriteResultEntity.builder()
                        .rewrittenQuery("quiz rewritten query")
                        .build()
        );
        DocumentWorkspaceServiceImpl service = newService(queryRewriteService);

        DocumentTaskResultEntity result = service.quiz("dws_001", null, 3, "short-answer");

        Assert.assertEquals("mock-answer", result.getAnswer());
        Assert.assertEquals("quiz rewritten query", result.getRewrittenQuery());
        Assert.assertEquals(1, result.getRetrievedChunks().size());

        ArgumentCaptor<DocumentQueryRewriteCommandEntity> captor = ArgumentCaptor.forClass(DocumentQueryRewriteCommandEntity.class);
        verify(queryRewriteService).rewrite(captor.capture());
        Assert.assertEquals("quiz", captor.getValue().getTaskType());
    }

    @Test
    public void shouldFallbackToOriginalQuestionWhenRewriteFails() {
        IQueryRewriteService queryRewriteService = mock(IQueryRewriteService.class);
        when(queryRewriteService.rewrite(any(DocumentQueryRewriteCommandEntity.class)))
                .thenThrow(new IllegalStateException("rewrite timeout"));
        DocumentWorkspaceServiceImpl service = newService(queryRewriteService);

        DocumentTaskResultEntity result = service.ask("dws_001", null, "  original question  ");

        Assert.assertEquals("original question", result.getRewrittenQuery());
    }

    private DocumentWorkspaceServiceImpl newService(IQueryRewriteService queryRewriteService) {
        IDocumentWorkspaceRepository repository = mock(IDocumentWorkspaceRepository.class);
        VectorStore documentVectorStore = mock(VectorStore.class);
        TokenTextSplitter tokenTextSplitter = mock(TokenTextSplitter.class);

        when(repository.existsWorkspace("dws_001")).thenReturn(true);
        when(repository.queryActiveDocumentIds("dws_001")).thenReturn(Set.of("doc_001"));
        when(documentVectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("matched workspace chunk", Map.of("spaceId", "dws_001", "docId", "doc_001", "fileName", "guide.md", "chunkIndex", "0"))
        ));

        return new DocumentWorkspaceServiceImpl(repository, documentVectorStore, tokenTextSplitter, queryRewriteService) {
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
