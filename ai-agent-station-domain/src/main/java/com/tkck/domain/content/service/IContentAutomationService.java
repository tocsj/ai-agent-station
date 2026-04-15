package com.tkck.domain.content.service;

import com.tkck.domain.content.model.entity.ContentCreateCommandEntity;
import com.tkck.domain.content.model.entity.ContentTaskEntity;
import com.tkck.domain.content.model.entity.ContentTaskStepEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;

import java.util.List;

public interface IContentAutomationService {

    ContentTaskEntity createTask(ContentCreateCommandEntity command);

    ContentTaskEntity queryTask(Long taskId);

    List<ContentTaskEntity> queryTaskHistory(Integer limit);

    List<ContentTaskStepEntity> queryTaskSteps(Long taskId);

    ContentTaskEntity markTaskRunning(Long taskId, String currentStep);

    void appendStep(Long taskId, int stepNo, String stepName, String stepStatus, String outputText, String metadataJson);

    void updateTaskArtifact(Long taskId, String fieldName, String fieldValue, String currentStep);

    void completeTask(Long taskId, String finalContent, String summaryText, PublishResultEntity publishResult);
}
