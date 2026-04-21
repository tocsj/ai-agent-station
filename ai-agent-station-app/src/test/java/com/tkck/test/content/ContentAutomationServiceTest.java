package com.tkck.test.content;

import com.tkck.app.content.ContentAutomationServiceImpl;
import com.tkck.domain.content.adapter.repository.IContentTaskRepository;
import com.tkck.domain.content.model.entity.ContentCreateCommandEntity;
import com.tkck.domain.content.model.entity.ContentTaskEntity;
import com.tkck.domain.content.model.entity.ContentTaskStepEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContentAutomationServiceTest {

    @Test
    public void shouldCreateQueryActiveHistoryAndListTaskSteps() {
        IContentTaskRepository repository = mock(IContentTaskRepository.class);
        ContentAutomationServiceImpl service = new ContentAutomationServiceImpl();
        ReflectionTestUtils.setField(service, "contentTaskRepository", repository);

        ContentTaskEntity createdTask = ContentTaskEntity.builder()
                .taskId(11L)
                .taskCode("ct_001")
                .executionMode("STRUCTURED_PLAN_EXECUTE")
                .topic("AI Agent Platform")
                .platform("dev.to")
                .style("professional")
                .keywords("AI,Agent")
                .channel("mock")
                .status("CREATED")
                .currentStep("CREATED")
                .build();
        ContentTaskEntity activeTaskRow = ContentTaskEntity.builder()
                .taskId(11L)
                .taskCode("ct_001")
                .executionMode("STRUCTURED_PLAN_EXECUTE")
                .topic("AI Agent Platform")
                .platform("dev.to")
                .style("professional")
                .keywords("AI,Agent")
                .channel("mock")
                .status("RUNNING")
                .currentStep("draft")
                .build();

        when(repository.saveTask(any(ContentTaskEntity.class))).thenReturn(createdTask);
        when(repository.queryTask(11L)).thenReturn(createdTask);
        when(repository.queryTaskHistory(20)).thenReturn(List.of(activeTaskRow));
        when(repository.queryLatestActiveTask()).thenReturn(activeTaskRow);
        when(repository.queryTaskSteps(11L)).thenReturn(List.of(
                ContentTaskStepEntity.builder()
                        .id(1L)
                        .taskId(11L)
                        .stepNo(1)
                        .stepName("topic_plan")
                        .stepStatus("COMPLETED")
                        .outputText("title")
                        .metadataJson("{}")
                        .build()
        ));

        ContentTaskEntity task = service.createTask(ContentCreateCommandEntity.builder()
                .topic("AI Agent Platform")
                .platform("dev.to")
                .style("professional")
                .keywords("AI,Agent")
                .channel("mock")
                .build());
        List<ContentTaskEntity> history = service.queryTaskHistory(20);
        ContentTaskEntity activeTask = service.queryActiveTask();
        List<ContentTaskStepEntity> steps = service.queryTaskSteps(11L);
        service.completeTask(11L, "final", "summary", PublishResultEntity.builder()
                .status("DRAFT_SAVED")
                .externalId("draft_001")
                .externalUrl("https://mock-publish.local/draft/draft_001")
                .build());

        Assert.assertEquals(Long.valueOf(11L), task.getTaskId());
        Assert.assertEquals("AI Agent Platform", task.getTopic());
        Assert.assertEquals(1, history.size());
        Assert.assertEquals(Long.valueOf(11L), history.get(0).getTaskId());
        Assert.assertEquals(Long.valueOf(11L), activeTask.getTaskId());
        Assert.assertEquals("RUNNING", activeTask.getStatus());
        Assert.assertEquals(1, steps.size());
        Assert.assertEquals("topic_plan", steps.get(0).getStepName());
        verify(repository).completeTask(eq(11L), eq("final"), eq("summary"), any(PublishResultEntity.class));
    }
}
