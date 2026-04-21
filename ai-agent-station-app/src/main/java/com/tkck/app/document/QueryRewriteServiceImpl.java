package com.tkck.app.document;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tkck.domain.agent.model.valobj.enums.AiAgentEnumVO;
import com.tkck.domain.document.model.entity.DocumentQueryRewriteCommandEntity;
import com.tkck.domain.document.model.entity.DocumentQueryRewriteResultEntity;
import com.tkck.domain.document.service.IQueryRewriteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class QueryRewriteServiceImpl implements IQueryRewriteService {

    static final String REWRITE_CLIENT_ID = "5401";
    static final String REWRITE_MODEL_CODE = "2008";

    @Resource
    private ApplicationContext applicationContext;

    @Override
    public DocumentQueryRewriteResultEntity rewrite(DocumentQueryRewriteCommandEntity command) {
        String originalQuestion = normalizeQuestion(command == null ? null : command.getOriginalQuestion());
        if (!StringUtils.hasText(originalQuestion)) {
            throw new IllegalArgumentException("document question is empty");
        }
        if (applicationContext == null) {
            return fallback(originalQuestion, "fallback_no_context");
        }
        try {
            ChatClient chatClient = (ChatClient) applicationContext.getBean(AiAgentEnumVO.AI_CLIENT.getBeanName(REWRITE_CLIENT_ID));
            String content = extractResponse(chatClient.prompt(
                    DocumentQueryRewritePromptBuilder.buildPrompt(
                            command.getTaskType(),
                            command.getTaskParams(),
                            originalQuestion
                    )).call().chatResponse());
            return DocumentQueryRewriteResultEntity.builder()
                    .originalQuestion(originalQuestion)
                    .rewrittenQuery(parseRewrittenQuery(content, originalQuestion))
                    .rewriteReason(parseRewriteReason(content))
                    .clientId(REWRITE_CLIENT_ID)
                    .modelCode(REWRITE_MODEL_CODE)
                    .build();
        } catch (Exception e) {
            log.warn("document query rewrite fallback, taskType={}, message={}",
                    command == null ? null : command.getTaskType(), e.getMessage());
            return fallback(originalQuestion, "fallback_exception");
        }
    }

    private String extractResponse(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text.trim();
    }

    private String parseRewrittenQuery(String content, String originalQuestion) {
        if (!StringUtils.hasText(content)) {
            return originalQuestion;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(content);
            String rewrittenQuery = jsonObject == null ? null : jsonObject.getString("rewrittenQuery");
            return StringUtils.hasText(rewrittenQuery) ? rewrittenQuery.trim() : originalQuestion;
        } catch (Exception ignored) {
            return content.trim();
        }
    }

    private String parseRewriteReason(String content) {
        if (!StringUtils.hasText(content)) {
            return "fallback_original";
        }
        try {
            JSONObject jsonObject = JSON.parseObject(content);
            String reason = jsonObject == null ? null : jsonObject.getString("rewriteReason");
            return StringUtils.hasText(reason) ? reason.trim() : "llm_rewrite";
        } catch (Exception ignored) {
            return "llm_rewrite";
        }
    }

    private DocumentQueryRewriteResultEntity fallback(String originalQuestion, String reason) {
        return DocumentQueryRewriteResultEntity.builder()
                .originalQuestion(originalQuestion)
                .rewrittenQuery(originalQuestion)
                .rewriteReason(reason)
                .clientId(REWRITE_CLIENT_ID)
                .modelCode(REWRITE_MODEL_CODE)
                .build();
    }

    private String normalizeQuestion(String originalQuestion) {
        return originalQuestion == null ? null : originalQuestion.trim();
    }
}
