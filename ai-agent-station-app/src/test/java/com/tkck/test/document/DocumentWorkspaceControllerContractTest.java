package com.tkck.test.document;

import com.tkck.api.dto.DocumentTaskRecordResponseDTO;
import com.tkck.api.dto.DocumentWorkspaceDetailResponseDTO;
import com.tkck.api.response.Response;
import com.tkck.domain.document.model.entity.DocumentFileEntity;
import com.tkck.domain.document.model.entity.DocumentTaskRecordEntity;
import com.tkck.domain.document.model.entity.DocumentWorkspaceDetailEntity;
import com.tkck.domain.document.service.IDocumentWorkspaceService;
import com.tkck.trigger.http.DocumentWorkspaceController;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DocumentWorkspaceControllerContractTest {

    @Test
    public void shouldQueryActiveWorkspaceAndRecentTasksForRestore() {
        IDocumentWorkspaceService documentWorkspaceService = mock(IDocumentWorkspaceService.class);
        DocumentWorkspaceController controller = new DocumentWorkspaceController();
        ReflectionTestUtils.setField(controller, "documentWorkspaceService", documentWorkspaceService);

        when(documentWorkspaceService.queryActiveWorkspace()).thenReturn(DocumentWorkspaceDetailEntity.builder()
                .workspaceId("dws_001")
                .workspaceName("workspace")
                .description("desc")
                .status("1")
                .documentCount(1)
                .documents(List.of(DocumentFileEntity.builder()
                        .docId("doc_001")
                        .workspaceId("dws_001")
                        .fileName("a.txt")
                        .fileType("txt")
                        .chunkCount(1)
                        .parseStatus("COMPLETED")
                        .vectorStatus("COMPLETED")
                        .build()))
                .build());
        when(documentWorkspaceService.queryRecentTasks("dws_001", 10)).thenReturn(List.of(DocumentTaskRecordEntity.builder()
                .taskId(1L)
                .workspaceId("dws_001")
                .docId("doc_001")
                .mode("ask")
                .question("question")
                .answer("answer")
                .status("SUCCESS")
                .build()));

        Response<DocumentWorkspaceDetailResponseDTO> active = controller.activeWorkspace();
        Response<List<DocumentTaskRecordResponseDTO>> recent = controller.recentTasks("dws_001", 10);

        Assert.assertEquals("0000", active.getCode());
        Assert.assertEquals("dws_001", active.getData().getWorkspaceId());
        Assert.assertEquals(1, active.getData().getDocuments().size());
        Assert.assertEquals(1, recent.getData().size());
        Assert.assertEquals("ask", recent.getData().get(0).getMode());
        Assert.assertEquals("answer", recent.getData().get(0).getAnswer());
    }
}
