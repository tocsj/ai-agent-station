package com.tkck.test.content;

import com.tkck.app.content.publish.MockPublishAdapter;
import com.tkck.app.content.workflow.ContentWorkflowContext;
import com.tkck.app.content.workflow.PublishExecutorNode;
import com.tkck.domain.audit.service.IAuditMonitoringService;
import com.tkck.domain.content.model.entity.ContentTaskEntity;
import com.tkck.domain.content.model.entity.ContentPublishRecordEntity;
import com.tkck.domain.content.model.entity.PublishCommandEntity;
import com.tkck.domain.content.service.IContentPublishChannelService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class PublishExecutorNodeTest {

    @Test
    public void shouldRecordPublishAttempt() {
        IContentPublishChannelService channelService = mock(IContentPublishChannelService.class);
        IAuditMonitoringService auditMonitoringService = mock(IAuditMonitoringService.class);
        PublishExecutorNode node = new PublishExecutorNode(List.of(new MockPublishAdapter()), channelService);
        setField(node, "auditMonitoringService", auditMonitoringService);
        ContentWorkflowContext context = ContentWorkflowContext.builder()
                .task(ContentTaskEntity.builder()
                        .taskId(21L)
                        .channel("mock")
                        .build())
                .build();
        context.setPublishCommand(PublishCommandEntity.builder()
                .channel("mock")
                .action("save_draft")
                .title("标题")
                .content("正文")
                .taskId(21L)
                .build());

        String output = node.apply(context);

        ArgumentCaptor<ContentPublishRecordEntity> captor = ArgumentCaptor.forClass(ContentPublishRecordEntity.class);
        verify(channelService).recordPublishAttempt(captor.capture());
        Assert.assertEquals("DRAFT_SAVED", captor.getValue().getStatus());
        Assert.assertTrue(output.contains("status=DRAFT_SAVED"));
    }
}
