package com.tkck.infrastructure.dao;

import com.tkck.infrastructure.dao.po.ContentTaskPO;
import com.tkck.infrastructure.dao.po.ContentTaskStepPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IContentTaskDao {

    int insertTask(ContentTaskPO task);

    ContentTaskPO queryTaskByTaskCode(@Param("taskCode") String taskCode);

    ContentTaskPO queryTaskById(@Param("taskId") Long taskId);

    ContentTaskPO queryLatestActiveTask();

    List<ContentTaskPO> queryTaskHistory(@Param("limit") int limit);

    List<ContentTaskStepPO> queryTaskSteps(@Param("taskId") Long taskId);

    int updateTaskStatus(@Param("taskId") Long taskId,
                         @Param("status") String status,
                         @Param("currentStep") String currentStep);

    int insertTaskStep(ContentTaskStepPO step);

    int updateTaskArtifact(@Param("taskId") Long taskId,
                           @Param("columnName") String columnName,
                           @Param("fieldValue") String fieldValue,
                           @Param("currentStep") String currentStep);

    int completeTask(@Param("taskId") Long taskId,
                     @Param("finalContent") String finalContent,
                     @Param("summaryText") String summaryText,
                     @Param("publishStatus") String publishStatus,
                     @Param("publishExternalId") String publishExternalId,
                     @Param("publishExternalUrl") String publishExternalUrl);
}
