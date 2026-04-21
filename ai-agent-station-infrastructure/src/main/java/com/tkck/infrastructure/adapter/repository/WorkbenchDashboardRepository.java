package com.tkck.infrastructure.adapter.repository;

import com.tkck.domain.workbench.adapter.repository.IWorkbenchDashboardRepository;
import com.tkck.domain.workbench.model.entity.WorkbenchAgentCardEntity;
import com.tkck.domain.workbench.model.entity.WorkbenchRecentRunEntity;
import com.tkck.infrastructure.dao.IWorkbenchDashboardDao;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class WorkbenchDashboardRepository implements IWorkbenchDashboardRepository {

    private static final List<String> SUPPORTED_TASK_TYPES = List.of(
            "resume_evaluation",
            "resume_interview",
            "document_workspace",
            "content_automation"
    );

    @Resource
    private IWorkbenchDashboardDao workbenchDashboardDao;

    @Override
    public List<WorkbenchAgentCardEntity> queryAgentCards(String range) {
        List<Map<String, Object>> rows = workbenchDashboardDao.queryAgentCardRows(normalizeRange(range));
        Map<String, Map<String, Object>> rowMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            rowMap.put(stringValue(row.get("task_type")), row);
        }
        List<WorkbenchAgentCardEntity> cards = new ArrayList<>();
        for (String taskType : SUPPORTED_TASK_TYPES) {
            Map<String, Object> row = rowMap.get(taskType);
            int total = intValue(row == null ? null : row.get("task_total"));
            int success = intValue(row == null ? null : row.get("success_total"));
            cards.add(WorkbenchAgentCardEntity.builder()
                    .taskType(taskType)
                    .taskTypeName(taskTypeName(taskType))
                    .description(taskDescription(taskType))
                    .routePath(routePath(taskType))
                    .taskTotal(total)
                    .successRate(rate(success, total))
                    .lastRunTime(stringValue(row == null ? null : row.get("last_run_time")))
                    .build());
        }
        return cards;
    }

    @Override
    public List<WorkbenchRecentRunEntity> queryRecentRuns(int limit) {
        return workbenchDashboardDao.queryRecentRunRows(limit).stream().map(row -> {
            String taskType = stringValue(row.get("task_type"));
            String taskSubType = stringValue(row.get("task_sub_type"));
            String taskId = stringValue(row.get("task_id"));
            String traceId = stringValue(row.get("trace_id"));
            return WorkbenchRecentRunEntity.builder()
                    .traceId(traceId)
                    .displayTaskId(buildDisplayTaskId(taskId, traceId))
                    .taskId(taskId)
                    .taskType(taskType)
                    .taskTypeName(taskTypeName(taskType))
                    .taskSubType(taskSubType)
                    .taskSubTypeName(taskSubTypeName(taskType, taskSubType))
                    .durationMs(longValue(row.get("total_duration_ms")))
                    .totalTokens(longValue(row.get("total_tokens")))
                    .status(stringValue(row.get("status")))
                    .statusText(statusText(stringValue(row.get("status"))))
                    .lastTime(stringValue(row.get("last_time")))
                    .detailTraceId(traceId)
                    .build();
        }).toList();
    }

    private String normalizeRange(String range) {
        if ("today".equalsIgnoreCase(range) || "30d".equalsIgnoreCase(range)) {
            return range.toLowerCase();
        }
        return "7d";
    }

    private String taskTypeName(String taskType) {
        return switch (taskType) {
            case "resume_evaluation" -> "简历评估";
            case "resume_interview" -> "模拟面试";
            case "document_workspace" -> "文档知识";
            case "content_automation" -> "内容自动化";
            default -> "未知任务";
        };
    }

    private String taskDescription(String taskType) {
        return switch (taskType) {
            case "resume_evaluation" -> "上传简历自动抽取信息、多维分析、深度洞察";
            case "resume_interview" -> "根据候选人简历生成动态上下文闭环面试";
            case "document_workspace" -> "支持多文档聚合检索、问答与深度理解";
            case "content_automation" -> "定制化流水线，从选题到发布全自动";
            default -> "";
        };
    }

    private String routePath(String taskType) {
        return switch (taskType) {
            case "resume_evaluation" -> "/resume-evaluation";
            case "resume_interview" -> "/resume-interview";
            case "document_workspace" -> "/document-workspace";
            case "content_automation" -> "/content-automation";
            default -> "/";
        };
    }

    private String taskSubTypeName(String taskType, String taskSubType) {
        if (taskSubType == null || taskSubType.isBlank()) {
            return null;
        }
        if ("document_workspace".equals(taskType)) {
            return switch (taskSubType) {
                case "ask" -> "文档提问";
                case "summary" -> "文档摘要";
                case "followup" -> "生成追问";
                case "quiz" -> "生成测验";
                default -> taskSubType;
            };
        }
        if ("content_automation".equals(taskType)) {
            return switch (taskSubType) {
                case "topic_plan" -> "选题规划";
                case "outline" -> "大纲生成";
                case "draft" -> "生成初稿";
                case "polish" -> "正文润色";
                case "compliance" -> "合规审核";
                case "publish_plan" -> "发布规划";
                case "publish_execute" -> "发布执行";
                case "publish_summary" -> "发布总结";
                default -> taskSubType;
            };
        }
        if ("resume_evaluation".equals(taskType) && "evaluation".equals(taskSubType)) {
            return "简历评估";
        }
        if ("resume_interview".equals(taskType) && "round_answer".equals(taskSubType)) {
            return "面试追问";
        }
        return taskSubType;
    }

    private String statusText(String status) {
        return switch (status) {
            case "SUCCESS" -> "成功";
            case "FAILED" -> "失败";
            case "RUNNING" -> "进行中";
            case "DEGRADED" -> "降级完成";
            default -> status;
        };
    }

    private String buildDisplayTaskId(String taskId, String traceId) {
        if (taskId != null && !taskId.isBlank()) {
            return "TSK-" + taskId;
        }
        if (traceId == null || traceId.isBlank()) {
            return "TSK-UNKNOWN";
        }
        return "TSK-" + traceId.substring(Math.max(0, traceId.length() - 6)).toUpperCase();
    }

    private int intValue(Object value) {
        if (value == null) {
            return 0;
        }
        return ((Number) value).intValue();
    }

    private long longValue(Object value) {
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private double rate(int success, int total) {
        if (total <= 0) {
            return 0D;
        }
        return Math.round(success * 10000.0 / total) / 100.0;
    }
}
