package com.tkck.test.document;

import com.tkck.app.document.DocumentWorkspaceServiceImpl;
import com.tkck.domain.document.adapter.repository.IDocumentWorkspaceRepository;
import com.tkck.domain.document.model.entity.DocumentWorkspaceEntity;
import com.tkck.domain.document.service.IQueryRewriteService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DocumentWorkspaceListServiceTest {

    @Test
    public void shouldReturnWorkspaceHistoryOrderedByUpdateTime() {
        IDocumentWorkspaceRepository repository = mock(IDocumentWorkspaceRepository.class);
        VectorStore documentVectorStore = mock(VectorStore.class);
        TokenTextSplitter tokenTextSplitter = mock(TokenTextSplitter.class);
        IQueryRewriteService queryRewriteService = mock(IQueryRewriteService.class);

        when(repository.queryWorkspaceList()).thenReturn(List.of(
                DocumentWorkspaceEntity.builder()
                        .workspaceId("dws_new")
                        .workspaceName("new-space")
                        .description("latest")
                        .status("1")
                        .documentCount(2)
                        .updateTime("2026-04-15T10:00")
                        .build(),
                DocumentWorkspaceEntity.builder()
                        .workspaceId("dws_old")
                        .workspaceName("old-space")
                        .description("older")
                        .status("1")
                        .documentCount(1)
                        .updateTime("2026-04-14T10:00")
                        .build()
        ));

        DocumentWorkspaceServiceImpl service = new DocumentWorkspaceServiceImpl(repository, documentVectorStore, tokenTextSplitter, queryRewriteService);

        List<DocumentWorkspaceEntity> result = service.listWorkspaces();

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("dws_new", result.get(0).getWorkspaceId());
        Assert.assertEquals(Integer.valueOf(2), result.get(0).getDocumentCount());
        Assert.assertNotNull(result.get(0).getUpdateTime());
    }
}
