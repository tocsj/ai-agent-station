package com.tkck.app.content.workflow;

import com.tkck.app.content.ContentChatClientFactory;
import com.tkck.domain.audit.model.entity.AuditLlmCallMetricEntity;
import com.tkck.domain.audit.service.IAuditMonitoringService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

@Slf4j
public abstract class AbstractContentWorkflowNode implements ContentWorkflowNode {

    @Resource
    protected ContentChatClientFactory contentChatClientFactory;
    @Resource
    protected IAuditMonitoringService auditMonitoringService;

    protected String generate(ContentWorkflowContext context, String prompt) {
        if (contentChatClientFactory == null) {
            return prompt;
        }
        long startTime = System.currentTimeMillis();
        ChatClient chatClient = contentChatClientFactory.getClient(clientId());
        try {
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
            String content = response == null || response.getResult() == null || response.getResult().getOutput() == null
                    ? ""
                    : safe(response.getResult().getOutput().getText());
            Usage usage = response == null || response.getMetadata() == null ? null : response.getMetadata().getUsage();
            recordLlmCall(context, usage, System.currentTimeMillis() - startTime, "SUCCESS", null, null);
            log.info("内容节点生成完成, node={}, clientId={}, promptLength={}, resultLength={}",
                    getClass().getSimpleName(),
                    clientId(),
                    prompt == null ? 0 : prompt.length(),
                    content.length());
            return content;
        } catch (Exception e) {
            recordLlmCall(context, null, System.currentTimeMillis() - startTime, "FAILED", e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    private void recordLlmCall(ContentWorkflowContext context,
                               Usage usage,
                               long durationMs,
                               String status,
                               String errorCode,
                               String errorMessage) {
        if (auditMonitoringService == null || context == null || context.getTask() == null) {
            return;
        }
        auditMonitoringService.recordLlmCall(AuditLlmCallMetricEntity.builder()
                .traceId(context.getValue("traceId"))
                .taskType("content_automation")
                .taskId(String.valueOf(context.getTask().getTaskId()))
                .sessionId(context.getValue("sessionId"))
                .stepName(stepName())
                .stage(stage().name())
                .clientId(clientId())
                .modelCode(modelCode(clientId()))
                .status(status)
                .durationMs(durationMs)
                .promptTokens(usage == null ? 0L : usage.getPromptTokens())
                .completionTokens(usage == null ? 0L : usage.getCompletionTokens())
                .totalTokens(usage == null ? 0L : usage.getTotalTokens())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .location(getClass().getSimpleName() + "#apply")
                .build());
        log.info("内容节点 LLM 调用完成, node={}, stepName={}, status={}, promptTokens={}, completionTokens={}, totalTokens={}, durationMs={}",
                getClass().getSimpleName(),
                stepName(),
                status,
                usage == null ? 0L : usage.getPromptTokens(),
                usage == null ? 0L : usage.getCompletionTokens(),
                usage == null ? 0L : usage.getTotalTokens(),
                durationMs);
    }

    @Override
    public String clientId() {
        return "5301";
    }

    protected String safe(String value) {
        return value == null ? "" : value;
    }

    protected String modelCode(String clientId) {
        if ("5301".equals(clientId)) {
            return "2007";
        }
        if ("5302".equals(clientId)) {
            return "2008";
        }
        return clientId == null ? "" : clientId;
    }
}
