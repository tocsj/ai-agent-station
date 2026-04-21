package com.tkck.domain.content.adapter.repository;

import com.tkck.domain.content.model.entity.ContentTaskEntity;
import com.tkck.domain.content.model.entity.ContentTaskStepEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;

import java.util.List;

public interface IContentTaskRepository {

    ContentTaskEntity saveTask(ContentTaskEntity task);

    ContentTaskEntity queryTask(Long taskId);

    ContentTaskEntity queryLatestActiveTask();

    List<ContentTaskEntity> queryTaskHistory(int limit);

    List<ContentTaskStepEntity> queryTaskSteps(Long taskId);

    void updateTaskStatus(Long taskId, String status, String currentStep);

    void saveTaskStep(ContentTaskStepEntity step);

    void updateTaskArtifact(Long taskId, String fieldName, String fieldValue, String currentStep);

    void completeTask(Long taskId, String finalContent, String summaryText, PublishResultEntity publishResult);
}
