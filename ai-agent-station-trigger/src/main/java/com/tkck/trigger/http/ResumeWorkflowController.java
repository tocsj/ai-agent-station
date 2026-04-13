package com.tkck.trigger.http;

import com.tkck.api.dto.ResumeEvaluateRequestDTO;
import com.tkck.api.dto.ResumeInterviewAnswerRequestDTO;
import com.tkck.api.dto.ResumeInterviewStartRequestDTO;
import com.tkck.api.dto.ResumeInterviewStartResponseDTO;
import com.tkck.api.dto.ResumeUploadResponseDTO;
import com.tkck.api.response.Response;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.service.execute.IExecuteStrategy;
import com.tkck.domain.resume.model.entity.ResumeInterviewStartEntity;
import com.tkck.domain.resume.model.entity.ResumeUploadResultEntity;
import com.tkck.domain.resume.service.IResumeWorkflowService;
import com.tkck.types.enums.ResponseCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import javax.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@RestController
@RequestMapping("/api/v1/resume")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class ResumeWorkflowController {

    @Resource(name = "autoAgentExecuteStrategy")
    private IExecuteStrategy autoAgentExecuteStrategy;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private IResumeWorkflowService resumeWorkflowService;

    @PostMapping("/upload")
    public Response<ResumeUploadResponseDTO> upload(@RequestParam("file") MultipartFile file) throws Exception {
        ResumeUploadResultEntity result = resumeWorkflowService.uploadResume(file);
        return Response.<ResumeUploadResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(ResumeUploadResponseDTO.builder()
                        .resumeId(result.getResumeId())
                        .knowledgeSpaceId(result.getKnowledgeSpaceId())
                        .knowledgeTag(result.getKnowledgeTag())
                        .fileName(result.getFileName())
                        .chunkCount(result.getChunkCount())
                        .build())
                .build();
    }

    @PostMapping("/evaluate/stream")
    public ResponseBodyEmitter evaluate(@RequestBody ResumeEvaluateRequestDTO request, HttpServletResponse response) {
        ExecuteCommandEntity command = resumeWorkflowService.buildResumeEvaluationCommand(
                request.getResumeId(),
                request.getKnowledgeSpaceId(),
                request.getQuestion(),
                request.getSessionId(),
                request.getMaxStep());
        return executeWithSse(command, response);
    }

    @PostMapping("/interview/start")
    public Response<ResumeInterviewStartResponseDTO> startInterview(@RequestBody ResumeInterviewStartRequestDTO request) throws Exception {
        ResumeInterviewStartEntity result = resumeWorkflowService.startInterview(request.getResumeId(), request.getKnowledgeSpaceId());
        return Response.<ResumeInterviewStartResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(ResumeInterviewStartResponseDTO.builder()
                        .interviewSessionId(result.getInterviewSessionId())
                        .currentRound(result.getCurrentRound())
                        .openingQuestions(result.getOpeningQuestions())
                        .build())
                .build();
    }

    @PostMapping("/interview/answer/stream")
    public ResponseBodyEmitter answerInterview(@RequestBody ResumeInterviewAnswerRequestDTO request, HttpServletResponse response) throws Exception {
        ExecuteCommandEntity command = resumeWorkflowService.buildInterviewAnswerCommand(
                request.getInterviewSessionId(),
                request.getRoundNo(),
                request.getAnswer(),
                request.getSessionId(),
                request.getMaxStep());
        return executeWithSse(command, response);
    }

    private ResponseBodyEmitter executeWithSse(ExecuteCommandEntity executeCommandEntity, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        ResponseBodyEmitter emitter = new ResponseBodyEmitter(Long.MAX_VALUE);
        threadPoolExecutor.execute(() -> {
            try {
                autoAgentExecuteStrategy.execute(executeCommandEntity, emitter);
            } catch (Exception e) {
                log.error("resume workflow execute error", e);
                try {
                    emitter.send("resume workflow execute error: " + e.getMessage());
                } catch (Exception sendEx) {
                    log.error("resume workflow send error", sendEx);
                }
            } finally {
                try {
                    emitter.complete();
                } catch (Exception completeEx) {
                    log.error("resume workflow complete error", completeEx);
                }
            }
        });
        return emitter;
    }
}
