package com.tkck.app.content;

import com.alibaba.fastjson.JSON;
import com.tkck.domain.agent.model.valobj.ExecutionMode;
import com.tkck.domain.content.adapter.repository.IContentTaskRepository;
import com.tkck.domain.content.model.entity.ContentCreateCommandEntity;
import com.tkck.domain.content.model.entity.ContentTaskEntity;
import com.tkck.domain.content.model.entity.ContentTaskStepEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import com.tkck.domain.content.service.IContentAutomationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ContentAutomationServiceImpl implements IContentAutomationService {

    @Resource
    private IContentTaskRepository contentTaskRepository;

    @Override
    public ContentTaskEntity createTask(ContentCreateCommandEntity command) {
        String taskCode = "ct_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return contentTaskRepository.saveTask(ContentTaskEntity.builder()
                .taskCode(taskCode)
                .executionMode(ExecutionMode.STRUCTURED_PLAN_EXECUTE.name())
                .topic(command.getTopic())
                .platform(command.getPlatform())
                .style(command.getStyle())
                .keywords(command.getKeywords())
                .channel(command.getChannel())
                .status("CREATED")
                .currentStep("CREATED")
                .build());
    }

    @Override
    public ContentTaskEntity queryTask(Long taskId) {
        return contentTaskRepository.queryTask(taskId);
    }

    @Override
    public ContentTaskEntity queryActiveTask() {
        return contentTaskRepository.queryLatestActiveTask();
    }

    @Override
    public List<ContentTaskEntity> queryTaskHistory(Integer limit) {
        int size = limit == null ? 20 : Math.max(1, Math.min(limit, 50));
        return contentTaskRepository.queryTaskHistory(size);
    }

    @Override
    public List<ContentTaskStepEntity> queryTaskSteps(Long taskId) {
        return contentTaskRepository.queryTaskSteps(taskId);
    }

    @Override
    public ContentTaskEntity markTaskRunning(Long taskId, String currentStep) {
        contentTaskRepository.updateTaskStatus(taskId, "RUNNING", currentStep);
        return queryTask(taskId);
    }

    @Override
    public void appendStep(Long taskId, int stepNo, String stepName, String stepStatus, String outputText, String metadataJson) {
        contentTaskRepository.saveTaskStep(ContentTaskStepEntity.builder()
                .taskId(taskId)
                .stepNo(stepNo)
                .stepName(stepName)
                .stepStatus(stepStatus)
                .outputText(outputText)
                .metadataJson(metadataJson == null ? JSON.toJSONString(java.util.Map.of()) : metadataJson)
                .build());
    }

    @Override
    public void updateTaskArtifact(Long taskId, String fieldName, String fieldValue, String currentStep) {
        String columnName = switch (fieldName) {
            case "title" -> "title";
            case "outline_text" -> "outline_text";
            case "draft_content" -> "draft_content";
            case "final_content" -> "final_content";
            case "compliance_result" -> "compliance_result";
            case "summary_text" -> "summary_text";
            default -> throw new IllegalArgumentException("unsupported content task field: " + fieldName);
        };
        contentTaskRepository.updateTaskArtifact(taskId, columnName, fieldValue, currentStep);
    }

    @Override
    public void completeTask(Long taskId, String finalContent, String summaryText, PublishResultEntity publishResult) {
        contentTaskRepository.completeTask(taskId, finalContent, summaryText, publishResult);
    }
}
