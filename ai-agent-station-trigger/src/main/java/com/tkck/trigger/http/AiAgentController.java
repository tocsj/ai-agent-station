package com.tkck.trigger.http;

import com.alibaba.fastjson.JSON;
import com.tkck.api.IAiAgentService;
import com.tkck.api.dto.AutoAgentRequestDTO;
import com.tkck.domain.agent.model.entity.AutoAgentExecuteResultEntity;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.service.execute.IExecuteStrategy;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionErrorCode;
import com.tkck.trigger.http.sse.SafeSseEmitter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import javax.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AiAgentController implements IAiAgentService {

    @Resource(name = "autoAgentExecuteStrategy")
    private IExecuteStrategy autoAgentExecuteStrategy;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @RequestMapping(value = "auto_agent", method = RequestMethod.POST)
    public ResponseBodyEmitter autoAgent(@RequestBody AutoAgentRequestDTO request, HttpServletResponse response) {
        log.info("auto agent stream request start, request={}", JSON.toJSONString(request));
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        SafeSseEmitter emitter = new SafeSseEmitter(Long.MAX_VALUE);
        try {
            ExecuteCommandEntity executeCommandEntity = ExecuteCommandEntity.builder()
                    .aiAgentId(request.getAiAgentId())
                    .message(request.getMessage())
                    .maxStep(request.getMaxStep())
                    .sessionId(request.getSessionId())
                    .build();

            threadPoolExecutor.execute(() -> {
                try {
                    autoAgentExecuteStrategy.execute(executeCommandEntity, emitter);
                } catch (Exception e) {
                    log.error("auto agent execute error", e);
                    AutoAgentExecuteResultEntity errorResult = AutoAgentExecuteResultEntity.createErrorResult(
                            e.getMessage() == null ? "auto agent execute error" : e.getMessage(),
                            ExecutionErrorCode.SYSTEM_ERROR.getCode(),
                            "ROOT",
                            false,
                            false,
                            executeCommandEntity.getSessionId()
                    );
                    emitter.safeSend("data: " + JSON.toJSONString(errorResult) + "\n\n");
                } finally {
                    emitter.completeSafely();
                }
            });
            return emitter;
        } catch (Exception e) {
            log.error("auto agent request process error", e);
            AutoAgentExecuteResultEntity errorResult = AutoAgentExecuteResultEntity.createErrorResult(
                    e.getMessage() == null ? "request process error" : e.getMessage(),
                    ExecutionErrorCode.SYSTEM_ERROR.getCode(),
                    "ROOT",
                    false,
                    false,
                    request.getSessionId()
            );
            emitter.safeSend("data: " + JSON.toJSONString(errorResult) + "\n\n");
            emitter.completeSafely();
            return emitter;
        }
    }
}
