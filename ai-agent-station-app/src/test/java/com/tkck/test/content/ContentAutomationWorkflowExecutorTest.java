package com.tkck.test.content;

import com.tkck.app.content.publish.MockPublishAdapter;
import com.tkck.app.content.workflow.ComplianceReviewerNode;
import com.tkck.app.content.workflow.ContentAutomationWorkflowExecutor;
import com.tkck.app.content.workflow.DraftGeneratorNode;
import com.tkck.app.content.workflow.OutlineGeneratorNode;
import com.tkck.app.content.workflow.PolishVerifierNode;
import com.tkck.app.content.workflow.PublishExecutorNode;
import com.tkck.app.content.workflow.PublishPlannerNode;
import com.tkck.app.content.workflow.PublishSummarizerNode;
import com.tkck.app.content.workflow.TopicPlannerNode;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionResilienceCoordinator;
import com.tkck.domain.content.model.entity.ContentTaskEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import com.tkck.domain.content.service.IContentAutomationService;
import com.tkck.domain.content.service.IContentPublishChannelService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ContentAutomationWorkflowExecutorTest {

    @Test
    public void shouldExecuteAllWorkflowStagesAndCompleteTask() throws Exception {
        IContentAutomationService contentAutomationService = mock(IContentAutomationService.class);
        ContentTaskEntity task = ContentTaskEntity.builder()
                .taskId(9L)
                .taskCode("ct_009")
                .topic("AI Agent 编排平台")
                .platform("公众号")
                .style("专业")
                .keywords("AI,Agent,Java")
                .channel("mock")
                .build();
        when(contentAutomationService.queryTask(9L)).thenReturn(task);
        when(contentAutomationService.markTaskRunning(9L, "topic_plan")).thenReturn(task);
        IContentPublishChannelService publishChannelService = mock(IContentPublishChannelService.class);

        ContentAutomationWorkflowExecutor executor = new ContentAutomationWorkflowExecutor(List.of(
                new TopicPlannerNode(),
                new OutlineGeneratorNode(),
                new DraftGeneratorNode(),
                new PolishVerifierNode(),
                new ComplianceReviewerNode(),
                new PublishPlannerNode(),
                new PublishExecutorNode(List.of(new MockPublishAdapter()), publishChannelService),
                new PublishSummarizerNode()
        ));
        ReflectionTestUtils.setField(executor, "contentAutomationService", contentAutomationService);
        ReflectionTestUtils.setField(executor, "executionResilienceCoordinator", new ExecutionResilienceCoordinator());

        CapturingEmitter emitter = new CapturingEmitter();
        executor.execute(ExecuteCommandEntity.builder()
                .taskType("content_automation")
                .contentTaskId(9L)
                .sessionId("content-9")
                .build(), emitter);

        verify(contentAutomationService, times(8)).appendStep(eq(9L), anyInt(), anyString(), anyString(), anyString(), anyString());
        verify(contentAutomationService, atLeastOnce()).updateTaskArtifact(eq(9L), eq("title"), anyString(), eq("topic_plan"));
        verify(contentAutomationService).completeTask(eq(9L), anyString(), anyString(), any(PublishResultEntity.class));

        ArgumentCaptor<PublishResultEntity> publishCaptor = ArgumentCaptor.forClass(PublishResultEntity.class);
        verify(contentAutomationService).completeTask(eq(9L), anyString(), anyString(), publishCaptor.capture());
        assertEquals("DRAFT_SAVED", publishCaptor.getValue().getStatus());
        assertTrue(publishCaptor.getValue().getExternalUrl().contains("mock-publish.local"));
        assertTrue(emitter.events.size() >= 9);
        assertTrue(emitter.events.get(emitter.events.size() - 1).contains("content_complete"));
    }

    private static class CapturingEmitter extends ResponseBodyEmitter {
        private final List<String> events = new ArrayList<>();

        @Override
        public synchronized void send(Object object) throws IOException {
            events.add(String.valueOf(object));
        }
    }
}
