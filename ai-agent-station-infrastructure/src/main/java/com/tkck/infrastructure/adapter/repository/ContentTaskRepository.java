package com.tkck.infrastructure.adapter.repository;

import com.alibaba.fastjson.JSON;
import com.tkck.domain.content.adapter.repository.IContentTaskRepository;
import com.tkck.domain.content.model.entity.ContentTaskEntity;
import com.tkck.domain.content.model.entity.ContentTaskStepEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import com.tkck.infrastructure.dao.IContentTaskDao;
import com.tkck.infrastructure.dao.po.ContentTaskPO;
import com.tkck.infrastructure.dao.po.ContentTaskStepPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class ContentTaskRepository implements IContentTaskRepository {

    @Resource
    private IContentTaskDao contentTaskDao;

    @Override
    public ContentTaskEntity saveTask(ContentTaskEntity task) {
        contentTaskDao.insertTask(ContentTaskPO.builder()
                .taskCode(task.getTaskCode())
                .executionMode(task.getExecutionMode())
                .topic(task.getTopic())
                .platform(task.getPlatform())
                .style(task.getStyle())
                .keywords(task.getKeywords())
                .channel(task.getChannel())
                .status(task.getStatus())
                .currentStep(task.getCurrentStep())
                .build());
        ContentTaskPO saved = contentTaskDao.queryTaskByTaskCode(task.getTaskCode());
        return toTaskEntity(saved);
    }

    @Override
    public ContentTaskEntity queryTask(Long taskId) {
        return toTaskEntity(contentTaskDao.queryTaskById(taskId));
    }

    @Override
    public ContentTaskEntity queryLatestActiveTask() {
        return toTaskEntity(contentTaskDao.queryLatestActiveTask());
    }

    @Override
    public List<ContentTaskEntity> queryTaskHistory(int limit) {
        return contentTaskDao.queryTaskHistory(limit).stream()
                .map(this::toTaskEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContentTaskStepEntity> queryTaskSteps(Long taskId) {
        return contentTaskDao.queryTaskSteps(taskId).stream()
                .map(this::toStepEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void updateTaskStatus(Long taskId, String status, String currentStep) {
        contentTaskDao.updateTaskStatus(taskId, status, currentStep);
    }

    @Override
    public void saveTaskStep(ContentTaskStepEntity step) {
        contentTaskDao.insertTaskStep(ContentTaskStepPO.builder()
                .taskId(step.getTaskId())
                .stepNo(step.getStepNo())
                .stepName(step.getStepName())
                .stepStatus(step.getStepStatus())
                .outputText(step.getOutputText())
                .metadataJson(step.getMetadataJson() == null ? JSON.toJSONString(Map.of()) : step.getMetadataJson())
                .build());
    }

    @Override
    public void updateTaskArtifact(Long taskId, String fieldName, String fieldValue, String currentStep) {
        contentTaskDao.updateTaskArtifact(taskId, fieldName, fieldValue, currentStep);
    }

    @Override
    public void completeTask(Long taskId, String finalContent, String summaryText, PublishResultEntity publishResult) {
        contentTaskDao.completeTask(
                taskId,
                finalContent,
                summaryText,
                publishResult == null ? "NOT_EXECUTED" : publishResult.getStatus(),
                publishResult == null ? null : publishResult.getExternalId(),
                publishResult == null ? null : publishResult.getExternalUrl()
        );
    }

    private ContentTaskEntity toTaskEntity(ContentTaskPO row) {
        if (row == null) {
            return null;
        }
        return ContentTaskEntity.builder()
                .taskId(row.getId())
                .taskCode(row.getTaskCode())
                .executionMode(row.getExecutionMode())
                .topic(row.getTopic())
                .platform(row.getPlatform())
                .style(row.getStyle())
                .keywords(row.getKeywords())
                .channel(row.getChannel())
                .status(row.getStatus())
                .currentStep(row.getCurrentStep())
                .title(row.getTitle())
                .outlineText(row.getOutlineText())
                .draftContent(row.getDraftContent())
                .finalContent(row.getFinalContent())
                .complianceResult(row.getComplianceResult())
                .publishStatus(row.getPublishStatus())
                .publishExternalId(row.getPublishExternalId())
                .publishExternalUrl(row.getPublishExternalUrl())
                .summaryText(row.getSummaryText())
                .createTime(row.getCreateTime() == null ? null : String.valueOf(row.getCreateTime()))
                .updateTime(row.getUpdateTime() == null ? null : String.valueOf(row.getUpdateTime()))
                .build();
    }

    private ContentTaskStepEntity toStepEntity(ContentTaskStepPO row) {
        if (row == null) {
            return null;
        }
        return ContentTaskStepEntity.builder()
                .id(row.getId())
                .taskId(row.getTaskId())
                .stepNo(row.getStepNo())
                .stepName(row.getStepName())
                .stepStatus(row.getStepStatus())
                .outputText(row.getOutputText())
                .metadataJson(row.getMetadataJson())
                .createTime(row.getCreateTime() == null ? null : String.valueOf(row.getCreateTime()))
                .build();
    }
}
