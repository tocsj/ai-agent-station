package com.tkck.trigger.http;

import com.alibaba.fastjson.JSON;
import com.tkck.api.dto.ContentTaskCreateRequestDTO;
import com.tkck.api.dto.ContentTaskCreateResponseDTO;
import com.tkck.api.dto.ContentTaskDetailResponseDTO;
import com.tkck.api.dto.ContentTaskExecuteRequestDTO;
import com.tkck.api.dto.ContentTaskHistoryItemDTO;
import com.tkck.api.response.Response;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.model.valobj.ExecutionMode;
import com.tkck.domain.agent.service.execute.IExecuteStrategy;
import com.tkck.domain.content.model.entity.ContentCreateCommandEntity;
import com.tkck.domain.content.model.entity.ContentTaskEntity;
import com.tkck.domain.content.model.entity.ContentTaskStepEntity;
import com.tkck.domain.content.model.entity.ContentTaskStreamEventEntity;
import com.tkck.domain.content.service.IContentAutomationService;
import com.tkck.trigger.http.sse.SafeSseEmitter;
import com.tkck.types.enums.ResponseCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/content")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class ContentAutomationController {

    @Resource
    private IContentAutomationService contentAutomationService;

    @Resource(name = "autoAgentExecuteStrategy")
    private IExecuteStrategy autoAgentExecuteStrategy;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @PostMapping("/task/create")
    public Response<ContentTaskCreateResponseDTO> createTask(@RequestBody ContentTaskCreateRequestDTO request) {
        ContentTaskEntity task = contentAutomationService.createTask(ContentCreateCommandEntity.builder()
                .topic(request.getTopic())
                .platform(request.getPlatform())
                .style(request.getStyle())
                .keywords(request.getKeywords())
                .channel(request.getChannel())
                .build());
        return Response.<ContentTaskCreateResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(ContentTaskCreateResponseDTO.builder()
                        .taskId(task.getTaskId())
                        .taskCode(task.getTaskCode())
                        .executionMode(task.getExecutionMode())
                        .status(task.getStatus())
                        .currentStep(task.getCurrentStep())
                        .build())
                .build();
    }

    @PostMapping("/task/execute/stream")
    public ResponseBodyEmitter executeStream(@RequestBody ContentTaskExecuteRequestDTO request, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        SafeSseEmitter emitter = new SafeSseEmitter(Long.MAX_VALUE);
        ExecuteCommandEntity command = ExecuteCommandEntity.builder()
                .taskType("content_automation")
                .executionMode(ExecutionMode.STRUCTURED_PLAN_EXECUTE)
                .contentTaskId(request.getTaskId())
                .sessionId(request.getSessionId() == null || request.getSessionId().isBlank()
                        ? "content-" + request.getTaskId() : request.getSessionId())
                .maxStep(request.getMaxStep())
                .build();

        threadPoolExecutor.execute(() -> {
            try {
                autoAgentExecuteStrategy.execute(command, emitter);
            } catch (Exception e) {
                log.error("content automation execute error", e);
                try {
                    emitter.safeSend("data: " + JSON.toJSONString(ContentTaskStreamEventEntity.builder()
                            .type("content_error")
                            .taskId(request.getTaskId())
                            .status("ERROR")
                            .content(e.getMessage() == null ? "content automation execute error" : e.getMessage())
                            .completed(true)
                            .timestamp(System.currentTimeMillis())
                            .build()) + "\n\n");
                } catch (Exception sendEx) {
                    log.error("content automation send error", sendEx);
                }
            } finally {
                emitter.completeSafely();
            }
        });
        return emitter;
    }

    @GetMapping("/task/{taskId:\\d+}")
    public Response<ContentTaskDetailResponseDTO> taskDetail(@PathVariable("taskId") Long taskId) {
        return Response.<ContentTaskDetailResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(toTaskDetail(contentAutomationService.queryTask(taskId)))
                .build();
    }

    @GetMapping("/task/active")
    public Response<ContentTaskDetailResponseDTO> activeTask() {
        ContentTaskEntity task = contentAutomationService.queryActiveTask();
        return Response.<ContentTaskDetailResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(task == null ? null : toTaskDetail(task))
                .build();
    }

    @GetMapping("/task/history")
    public Response<List<ContentTaskHistoryItemDTO>> taskHistory(@RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        return Response.<List<ContentTaskHistoryItemDTO>>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(contentAutomationService.queryTaskHistory(limit).stream()
                        .map(this::toHistoryItem)
                        .collect(Collectors.toList()))
                .build();
    }

    @GetMapping("/task/{taskId:\\d+}/steps")
    public Response<List<ContentTaskDetailResponseDTO.StepItem>> taskSteps(@PathVariable("taskId") Long taskId) {
        return Response.<List<ContentTaskDetailResponseDTO.StepItem>>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(contentAutomationService.queryTaskSteps(taskId).stream()
                        .map(this::toStepItem)
                        .collect(Collectors.toList()))
                .build();
    }

    private ContentTaskDetailResponseDTO toTaskDetail(ContentTaskEntity task) {
        return ContentTaskDetailResponseDTO.builder()
                .taskId(task.getTaskId())
                .taskCode(task.getTaskCode())
                .executionMode(task.getExecutionMode())
                .topic(task.getTopic())
                .platform(task.getPlatform())
                .style(task.getStyle())
                .keywords(task.getKeywords())
                .channel(task.getChannel())
                .status(task.getStatus())
                .currentStep(task.getCurrentStep())
                .title(task.getTitle())
                .outlineText(task.getOutlineText())
                .draftContent(task.getDraftContent())
                .finalContent(task.getFinalContent())
                .complianceResult(task.getComplianceResult())
                .publishStatus(task.getPublishStatus())
                .publishExternalId(task.getPublishExternalId())
                .publishExternalUrl(task.getPublishExternalUrl())
                .summaryText(task.getSummaryText())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
    }

    private ContentTaskHistoryItemDTO toHistoryItem(ContentTaskEntity task) {
        return ContentTaskHistoryItemDTO.builder()
                .taskId(task.getTaskId())
                .taskCode(task.getTaskCode())
                .topic(task.getTopic())
                .platform(task.getPlatform())
                .channel(task.getChannel())
                .status(task.getStatus())
                .currentStep(task.getCurrentStep())
                .title(task.getTitle())
                .publishStatus(task.getPublishStatus())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
    }

    private ContentTaskDetailResponseDTO.StepItem toStepItem(ContentTaskStepEntity step) {
        return ContentTaskDetailResponseDTO.StepItem.builder()
                .id(step.getId())
                .taskId(step.getTaskId())
                .stepNo(step.getStepNo())
                .stepName(step.getStepName())
                .stepStatus(step.getStepStatus())
                .outputText(step.getOutputText())
                .metadataJson(step.getMetadataJson())
                .createTime(step.getCreateTime())
                .build();
    }
}
