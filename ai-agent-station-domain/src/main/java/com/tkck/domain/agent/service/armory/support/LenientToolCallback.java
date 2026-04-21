package com.tkck.domain.agent.service.armory.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 对 MCP tool 调用进行“宽松化”处理的代理，主要用于 search 工具在缺少 queryBody
 * 或 queryBody 不是合法 JSON 对象时做兜底，避免直接抛出 Invalid arguments 错误。
 */
@Slf4j
public class LenientToolCallback implements ToolCallback {

    private static final int DEFAULT_SIZE = 20;

    private final ToolCallback delegate;

    private final boolean searchTool;

    private final String toolName;

    public LenientToolCallback(ToolCallback delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate callback must not be null");
        ToolDefinition definition = delegate.getToolDefinition();
        this.toolName = definition != null ? definition.name() : "";
        this.searchTool = this.toolName != null && this.toolName.toLowerCase().contains("search");
    }

    public static ToolCallback[] wrap(ToolCallback[] callbacks) {
        if (callbacks == null || callbacks.length == 0) {
            return callbacks;
        }
        ToolCallback[] wrapped = new ToolCallback[callbacks.length];
        for (int i = 0; i < callbacks.length; i++) {
            wrapped[i] = new LenientToolCallback(callbacks[i]);
        }
        return wrapped;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String request) {
        return delegate.call(adaptPayload(request));
    }

    @Override
    public String call(String request, ToolContext toolContext) {
        return delegate.call(adaptPayload(request), toolContext);
    }

    private String adaptPayload(String payload) {
        if (!searchTool) {
            return payload;
        }
        Map<String, Object> paramMap = safeToMap(payload);
        normalizeQueryBody(paramMap);
        return ModelOptionsUtils.toJsonString(paramMap);
    }

    private Map<String, Object> safeToMap(String payload) {
        if (StringUtils.isBlank(payload)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> data = ModelOptionsUtils.jsonToMap(payload);
            return data == null ? new LinkedHashMap<>() : new LinkedHashMap<>(data);
        } catch (Exception ex) {
            log.warn("Tool [{}] payload 解析失败，自动回退为空参数，原因: {}", toolName, ex.getMessage());
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private void normalizeQueryBody(Map<String, Object> params) {
        if (params == null) {
            return;
        }

        Object rawQueryBody = params.get("queryBody");
        Map<String, Object> queryBody = convertToMap(rawQueryBody);
        String keyword = extractKeyword(params);

        if (queryBody == null || queryBody.isEmpty()) {
            queryBody = defaultQueryBody(keyword);
        } else {
            queryBody.putIfAbsent("size", DEFAULT_SIZE);
            queryBody.putIfAbsent("sort", defaultSort());
            if (!queryBody.containsKey("query") || queryBody.get("query") == null) {
                queryBody.put("query", defaultQueryClause(keyword));
            }
        }

        params.put("queryBody", queryBody);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object value) {
        if (value instanceof Map<?, ?> existing) {
            Map<String, Object> copy = new LinkedHashMap<>();
            existing.forEach((k, v) -> copy.put(String.valueOf(k), v));
            return copy;
        }
        if (value instanceof String str && StringUtils.isNotBlank(str)) {
            try {
                Map<String, Object> parsed = ModelOptionsUtils.jsonToMap(str);
                return parsed == null ? null : new LinkedHashMap<>(parsed);
            } catch (Exception ex) {
                log.warn("Tool [{}] queryBody 解析 JSON 失败，自动回退默认 DSL，原因: {}", toolName, ex.getMessage());
                return null;
            }
        }
        return null;
    }

    private Map<String, Object> defaultQueryBody(String keyword) {
        Map<String, Object> queryBody = new LinkedHashMap<>();
        queryBody.put("size", DEFAULT_SIZE);
        queryBody.put("sort", defaultSort());
        queryBody.put("query", defaultQueryClause(keyword));
        return queryBody;
    }

    private Map<String, Object> defaultQueryClause(String keyword) {
        if (StringUtils.isNotBlank(keyword)) {
            return Map.of("match_phrase", Map.of("message", keyword));
        }
        return Map.of("match_all", Map.of());
    }

    private List<Map<String, Object>> defaultSort() {
        Map<String, Object> order = Map.of("order", "desc");
        Map<String, Object> timestampSort = Map.of("@timestamp", order);
        List<Map<String, Object>> sortList = new ArrayList<>();
        sortList.add(timestampSort);
        return sortList;
    }

    private String extractKeyword(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        String[] candidates = new String[]{"keyword", "keywords", "query", "term", "message"};
        for (String candidate : candidates) {
            Object value = params.get(candidate);
            if (value instanceof String str && StringUtils.isNotBlank(str)) {
                return str.trim();
            }
        }
        return "";
    }
}
