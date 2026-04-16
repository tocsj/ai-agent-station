package com.tkck.app.content;

import com.alibaba.fastjson.JSON;
import com.tkck.domain.agent.model.valobj.ExecutionMode;
import com.tkck.domain.content.model.entity.ContentCreateCommandEntity;
import com.tkck.domain.content.model.entity.ContentTaskEntity;
import com.tkck.domain.content.model.entity.ContentTaskStepEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import com.tkck.domain.content.service.IContentAutomationService;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ContentAutomationServiceImpl implements IContentAutomationService {

    @Resource(name = "mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbcTemplate;

    @Override
    public ContentTaskEntity createTask(ContentCreateCommandEntity command) {
        String taskCode = "ct_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mysqlJdbcTemplate.update("""
                INSERT INTO content_task (
                    task_code, execution_mode, topic, platform, style, keywords, channel, status, current_step
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                taskCode,
                ExecutionMode.STRUCTURED_PLAN_EXECUTE.name(),
                command.getTopic(),
                command.getPlatform(),
                command.getStyle(),
                command.getKeywords(),
                command.getChannel(),
                "CREATED",
                "CREATED");
        Long taskId = mysqlJdbcTemplate.queryForObject("SELECT id FROM content_task WHERE task_code = ?", Long.class, taskCode);
        return queryTask(taskId);
    }

    @Override
    public ContentTaskEntity queryTask(Long taskId) {
        Map<String, Object> row = mysqlJdbcTemplate.queryForMap("SELECT * FROM content_task WHERE id = ?", taskId);
        return toTaskEntity(row);
    }

    @Override
    public ContentTaskEntity queryActiveTask() {
        List<Map<String, Object>> rows = mysqlJdbcTemplate.queryForList("""
                SELECT *
                FROM content_task
                WHERE status IN ('CREATED', 'RUNNING', 'COMPLETED', 'FAILED')
                ORDER BY update_time DESC, id DESC
                LIMIT 1
                """);
        if (rows.isEmpty()) {
            return null;
        }
        return toTaskEntity(rows.get(0));
    }

    @Override
    public List<ContentTaskEntity> queryTaskHistory(Integer limit) {
        int size = limit == null ? 20 : Math.max(1, Math.min(limit, 50));
        return mysqlJdbcTemplate.queryForList(
                        "SELECT * FROM content_task ORDER BY update_time DESC, id DESC LIMIT ?",
                        size)
                .stream()
                .map(this::toTaskEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContentTaskStepEntity> queryTaskSteps(Long taskId) {
        return mysqlJdbcTemplate.queryForList(
                        "SELECT * FROM content_task_step WHERE task_id = ? ORDER BY step_no ASC, id ASC",
                        taskId)
                .stream()
                .map(this::toStepEntity)
                .collect(Collectors.toList());
    }

    @Override
    public ContentTaskEntity markTaskRunning(Long taskId, String currentStep) {
        mysqlJdbcTemplate.update(
                "UPDATE content_task SET status = ?, current_step = ?, update_time = NOW() WHERE id = ?",
                "RUNNING",
                currentStep,
                taskId
        );
        return queryTask(taskId);
    }

    @Override
    public void appendStep(Long taskId, int stepNo, String stepName, String stepStatus, String outputText, String metadataJson) {
        mysqlJdbcTemplate.update("""
                INSERT INTO content_task_step (task_id, step_no, step_name, step_status, output_text, metadata_json)
                VALUES (?, ?, ?, ?, ?, CAST(? AS JSON))
                """,
                taskId,
                stepNo,
                stepName,
                stepStatus,
                outputText,
                metadataJson == null ? JSON.toJSONString(Map.of()) : metadataJson
        );
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
        mysqlJdbcTemplate.update(
                "UPDATE content_task SET " + columnName + " = ?, current_step = ?, update_time = NOW() WHERE id = ?",
                fieldValue,
                currentStep,
                taskId
        );
    }

    @Override
    public void completeTask(Long taskId, String finalContent, String summaryText, PublishResultEntity publishResult) {
        mysqlJdbcTemplate.update("""
                UPDATE content_task
                SET status = ?,
                    current_step = ?,
                    final_content = ?,
                    summary_text = ?,
                    publish_status = ?,
                    publish_external_id = ?,
                    publish_external_url = ?,
                    update_time = NOW()
                WHERE id = ?
                """,
                "COMPLETED",
                "COMPLETED",
                finalContent,
                summaryText,
                publishResult == null ? "NOT_EXECUTED" : publishResult.getStatus(),
                publishResult == null ? null : publishResult.getExternalId(),
                publishResult == null ? null : publishResult.getExternalUrl(),
                taskId
        );
    }

    private ContentTaskEntity toTaskEntity(Map<String, Object> row) {
        return ContentTaskEntity.builder()
                .taskId(((Number) row.get("id")).longValue())
                .taskCode((String) row.get("task_code"))
                .executionMode((String) row.get("execution_mode"))
                .topic((String) row.get("topic"))
                .platform((String) row.get("platform"))
                .style((String) row.get("style"))
                .keywords((String) row.get("keywords"))
                .channel((String) row.get("channel"))
                .status((String) row.get("status"))
                .currentStep((String) row.get("current_step"))
                .title((String) row.get("title"))
                .outlineText((String) row.get("outline_text"))
                .draftContent((String) row.get("draft_content"))
                .finalContent((String) row.get("final_content"))
                .complianceResult((String) row.get("compliance_result"))
                .publishStatus((String) row.get("publish_status"))
                .publishExternalId((String) row.get("publish_external_id"))
                .publishExternalUrl((String) row.get("publish_external_url"))
                .summaryText((String) row.get("summary_text"))
                .createTime(row.get("create_time") == null ? null : String.valueOf(row.get("create_time")))
                .updateTime(row.get("update_time") == null ? null : String.valueOf(row.get("update_time")))
                .build();
    }

    private ContentTaskStepEntity toStepEntity(Map<String, Object> row) {
        return ContentTaskStepEntity.builder()
                .id(((Number) row.get("id")).longValue())
                .taskId(((Number) row.get("task_id")).longValue())
                .stepNo(((Number) row.get("step_no")).intValue())
                .stepName((String) row.get("step_name"))
                .stepStatus((String) row.get("step_status"))
                .outputText((String) row.get("output_text"))
                .metadataJson((String) row.get("metadata_json"))
                .createTime(row.get("create_time") == null ? null : String.valueOf(row.get("create_time")))
                .build();
    }
}
