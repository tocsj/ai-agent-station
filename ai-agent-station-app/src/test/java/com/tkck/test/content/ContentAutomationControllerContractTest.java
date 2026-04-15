package com.tkck.test.content;

import com.tkck.api.dto.ContentTaskCreateRequestDTO;
import com.tkck.api.dto.ContentTaskCreateResponseDTO;
import com.tkck.api.dto.ContentTaskDetailResponseDTO;
import com.tkck.api.dto.ContentTaskHistoryItemDTO;
import com.tkck.api.dto.ContentTaskExecuteRequestDTO;
import com.tkck.api.response.Response;
import com.tkck.domain.agent.service.execute.IExecuteStrategy;
import com.tkck.domain.content.model.entity.ContentTaskEntity;
import com.tkck.domain.content.model.entity.ContentTaskStepEntity;
import com.tkck.domain.content.service.IContentAutomationService;
import com.tkck.trigger.http.ContentAutomationController;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ContentAutomationControllerContractTest {

    @Test
    public void shouldCreateStreamAndQueryContentTask() throws Exception {
        IContentAutomationService contentAutomationService = mock(IContentAutomationService.class);
        IExecuteStrategy executeStrategy = mock(IExecuteStrategy.class);
        ContentAutomationController controller = new ContentAutomationController();
        ReflectionTestUtils.setField(controller, "contentAutomationService", contentAutomationService);
        ReflectionTestUtils.setField(controller, "autoAgentExecuteStrategy", executeStrategy);
        ReflectionTestUtils.setField(controller, "threadPoolExecutor", java.util.concurrent.Executors.newFixedThreadPool(1));

        when(contentAutomationService.createTask(any())).thenReturn(ContentTaskEntity.builder()
                .taskId(21L)
                .taskCode("ct_021")
                .executionMode("STRUCTURED_PLAN_EXECUTE")
                .status("CREATED")
                .currentStep("CREATED")
                .topic("Agent Runtime 2.0")
                .platform("公众号")
                .style("专业")
                .keywords("AI,Runtime")
                .channel("mock")
                .build());
        when(contentAutomationService.queryTask(21L)).thenReturn(ContentTaskEntity.builder()
                .taskId(21L)
                .taskCode("ct_021")
                .executionMode("STRUCTURED_PLAN_EXECUTE")
                .status("CREATED")
                .currentStep("CREATED")
                .topic("Agent Runtime 2.0")
                .platform("公众号")
                .style("专业")
                .keywords("AI,Runtime")
                .channel("mock")
                .build());
        when(contentAutomationService.queryTaskHistory(20)).thenReturn(List.of(
                ContentTaskEntity.builder()
                        .taskId(21L)
                        .taskCode("ct_021")
                        .topic("Agent Runtime 2.0")
                        .platform("公众号")
                        .channel("mock")
                        .status("COMPLETED")
                        .currentStep("COMPLETED")
                        .title("标题")
                        .publishStatus("DRAFT_SAVED")
                        .build()
        ));
        when(contentAutomationService.queryTaskSteps(21L)).thenReturn(List.of(
                ContentTaskStepEntity.builder()
                        .id(1L)
                        .taskId(21L)
                        .stepNo(1)
                        .stepName("topic_plan")
                        .stepStatus("COMPLETED")
                        .outputText("title")
                        .metadataJson("{}")
                        .build()
        ));
        doNothing().when(executeStrategy).execute(any(), any());

        Response<ContentTaskCreateResponseDTO> createResponse = controller.createTask(ContentTaskCreateRequestDTO.builder()
                .topic("Agent Runtime 2.0")
                .platform("公众号")
                .style("专业")
                .keywords("AI,Runtime")
                .channel("mock")
                .build());
        ResponseBodyEmitter emitter = controller.executeStream(ContentTaskExecuteRequestDTO.builder()
                .taskId(21L)
                .sessionId("content-21")
                .maxStep(8)
                .build(), mock(HttpServletResponse.class));
        Response<ContentTaskDetailResponseDTO> detailResponse = controller.taskDetail(21L);
        Response<List<ContentTaskHistoryItemDTO>> historyResponse = controller.taskHistory(20);
        Response<List<ContentTaskDetailResponseDTO.StepItem>> stepsResponse = controller.taskSteps(21L);

        Assert.assertEquals("0000", createResponse.getCode());
        Assert.assertEquals(Long.valueOf(21L), createResponse.getData().getTaskId());
        Assert.assertNotNull(emitter);
        Assert.assertEquals(Long.valueOf(21L), detailResponse.getData().getTaskId());
        Assert.assertEquals(1, historyResponse.getData().size());
        Assert.assertEquals(Long.valueOf(21L), historyResponse.getData().get(0).getTaskId());
        Assert.assertEquals(1, stepsResponse.getData().size());
        Assert.assertEquals("topic_plan", stepsResponse.getData().get(0).getStepName());
    }

    @Test
    public void shouldBindHistoryLimitRequestParamWithoutCompilerParameterMetadata() throws Exception {
        IContentAutomationService contentAutomationService = mock(IContentAutomationService.class);
        ContentAutomationController controller = new ContentAutomationController();
        ReflectionTestUtils.setField(controller, "contentAutomationService", contentAutomationService);

        when(contentAutomationService.queryTaskHistory(20)).thenReturn(List.of(
                ContentTaskEntity.builder()
                        .taskId(21L)
                        .taskCode("ct_021")
                        .topic("Agent Runtime 2.0")
                        .platform("dev.to")
                        .channel("devto")
                        .status("COMPLETED")
                        .currentStep("COMPLETED")
                        .title("发布结果")
                        .publishStatus("DRAFT_SAVED")
                        .build()
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/v1/content/task/history").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data[0].taskId").value(21))
                .andExpect(jsonPath("$.data[0].channel").value("devto"));
    }
}
