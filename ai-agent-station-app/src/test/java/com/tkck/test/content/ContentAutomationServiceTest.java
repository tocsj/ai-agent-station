package com.tkck.test.content;

import com.tkck.app.content.ContentAutomationServiceImpl;
import com.tkck.domain.content.model.entity.ContentCreateCommandEntity;
import com.tkck.domain.content.model.entity.ContentTaskEntity;
import com.tkck.domain.content.model.entity.ContentTaskStepEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentAutomationServiceTest {

    @Test
    public void shouldCreateQueryHistoryAndListTaskSteps() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ContentAutomationServiceImpl service = new ContentAutomationServiceImpl();
        ReflectionTestUtils.setField(service, "mysqlJdbcTemplate", jdbcTemplate);

        when(jdbcTemplate.queryForObject(eq("SELECT id FROM content_task WHERE task_code = ?"), eq(Long.class), anyString()))
                .thenReturn(11L);
        when(jdbcTemplate.queryForMap("SELECT * FROM content_task WHERE id = ?", 11L))
                .thenReturn(Map.of(
                        "id", 11L,
                        "task_code", "ct_001",
                        "execution_mode", "STRUCTURED_PLAN_EXECUTE",
                        "topic", "AI Agent 平台",
                        "platform", "公众号",
                        "style", "专业",
                        "keywords", "AI,Agent",
                        "channel", "mock",
                        "status", "CREATED",
                        "current_step", "CREATED"
                ));
        when(jdbcTemplate.queryForList("SELECT * FROM content_task ORDER BY update_time DESC, id DESC LIMIT ?", 20))
                .thenReturn(List.of(
                        Map.of(
                                "id", 11L,
                                "task_code", "ct_001",
                                "execution_mode", "STRUCTURED_PLAN_EXECUTE",
                                "topic", "AI Agent 平台",
                                "platform", "公众号",
                                "style", "专业",
                                "keywords", "AI,Agent",
                                "channel", "mock",
                                "status", "COMPLETED",
                                "current_step", "COMPLETED"
                        )
                ));
        when(jdbcTemplate.queryForList("SELECT * FROM content_task_step WHERE task_id = ? ORDER BY step_no ASC, id ASC", 11L))
                .thenReturn(List.of(
                        Map.of(
                                "id", 1L,
                                "task_id", 11L,
                                "step_no", 1,
                                "step_name", "topic_plan",
                                "step_status", "COMPLETED",
                                "output_text", "title",
                                "metadata_json", "{}"
                        )
                ));

        ContentTaskEntity task = service.createTask(ContentCreateCommandEntity.builder()
                .topic("AI Agent 平台")
                .platform("公众号")
                .style("专业")
                .keywords("AI,Agent")
                .channel("mock")
                .build());
        List<ContentTaskEntity> history = service.queryTaskHistory(20);
        List<ContentTaskStepEntity> steps = service.queryTaskSteps(11L);
        service.completeTask(11L, "final", "summary", PublishResultEntity.builder()
                .status("DRAFT_SAVED")
                .externalId("draft_001")
                .externalUrl("https://mock-publish.local/draft/draft_001")
                .build());

        Assert.assertEquals(Long.valueOf(11L), task.getTaskId());
        Assert.assertEquals("AI Agent 平台", task.getTopic());
        Assert.assertEquals(1, history.size());
        Assert.assertEquals(Long.valueOf(11L), history.get(0).getTaskId());
        Assert.assertEquals(1, steps.size());
        Assert.assertEquals("topic_plan", steps.get(0).getStepName());
    }
}
