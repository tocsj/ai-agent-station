package com.tkck.trigger.http;

import com.tkck.api.dto.ResumeEvaluateRequestDTO;
import com.tkck.api.dto.ResumeEvaluationTaskResponseDTO;
import com.tkck.api.dto.ResumeInterviewDetailResponseDTO;
import com.tkck.api.dto.ResumeInterviewAnswerRequestDTO;
import com.tkck.api.dto.ResumeInterviewStartRequestDTO;
import com.tkck.api.dto.ResumeInterviewStartResponseDTO;
import com.tkck.api.dto.ResumeProfileItemDTO;
import com.tkck.api.dto.ResumeUploadResponseDTO;
import com.tkck.api.response.Response;
import com.tkck.domain.agent.model.entity.AutoAgentExecuteResultEntity;
import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionErrorCode;
import com.tkck.domain.agent.service.execute.IExecuteStrategy;
import com.tkck.domain.resume.model.entity.ResumeInterviewDetailEntity;
import com.tkck.domain.resume.model.entity.ResumeInterviewRoundEntity;
import com.tkck.domain.resume.model.entity.ResumeInterviewStartEntity;
import com.tkck.domain.resume.model.entity.ResumeEvaluationTaskEntity;
import com.tkck.domain.resume.model.entity.ResumeUploadResultEntity;
import com.tkck.domain.resume.service.IResumeWorkflowService;
import com.tkck.trigger.http.sse.SafeSseEmitter;
import com.tkck.types.enums.ResponseCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

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
                        .totalRounds(result.getTotalRounds())
                        .status(result.getStatus())
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

    @GetMapping("/profile/recent")
    public Response<List<ResumeProfileItemDTO>> recentProfiles(@RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        return Response.<List<ResumeProfileItemDTO>>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(resumeWorkflowService.queryRecentResumes(limit).stream()
                        .map(this::toProfileItem)
                        .collect(Collectors.toList()))
                .build();
    }

    @GetMapping("/evaluation/active")
    public Response<ResumeEvaluationTaskResponseDTO> activeEvaluation() {
        ResumeEvaluationTaskEntity task = resumeWorkflowService.queryActiveEvaluationTask();
        return Response.<ResumeEvaluationTaskResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(task == null ? null : toEvaluationTask(task))
                .build();
    }

    @GetMapping("/evaluation/recent")
    public Response<List<ResumeEvaluationTaskResponseDTO>> recentEvaluations(@RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        return Response.<List<ResumeEvaluationTaskResponseDTO>>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(resumeWorkflowService.queryRecentEvaluationTasks(limit).stream()
                        .map(this::toEvaluationTask)
                        .collect(Collectors.toList()))
                .build();
    }

    @GetMapping("/interview/{interviewSessionId:\\d+}")
    public Response<ResumeInterviewDetailResponseDTO> interviewDetail(@PathVariable("interviewSessionId") Long interviewSessionId) {
        ResumeInterviewDetailEntity detail = resumeWorkflowService.queryInterviewDetail(interviewSessionId);
        return Response.<ResumeInterviewDetailResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(ResumeInterviewDetailResponseDTO.builder()
                        .interviewSessionId(detail.getInterviewSessionId())
                        .resumeId(detail.getResumeId())
                        .knowledgeSpaceId(detail.getKnowledgeSpaceId())
                        .sessionCode(detail.getSessionCode())
                        .currentRound(detail.getCurrentRound())
                        .totalRounds(detail.getTotalRounds())
                        .status(detail.getStatus())
                        .openingQuestions(detail.getOpeningQuestions())
                        .finalReport(detail.getFinalReport())
                        .rounds(detail.getRounds() == null ? null : detail.getRounds().stream().map(this::toRoundItem).collect(Collectors.toList()))
                        .build())
                .build();
    }

    @GetMapping("/interview/active")
    public Response<ResumeInterviewDetailResponseDTO> activeInterview() {
        ResumeInterviewDetailEntity detail = resumeWorkflowService.queryActiveInterviewDetail();
        return Response.<ResumeInterviewDetailResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(detail == null ? null : toInterviewDetail(detail))
                .build();
    }

    private ResponseBodyEmitter executeWithSse(ExecuteCommandEntity executeCommandEntity, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        SafeSseEmitter emitter = new SafeSseEmitter(Long.MAX_VALUE);
        threadPoolExecutor.execute(() -> {
            try {
                autoAgentExecuteStrategy.execute(executeCommandEntity, emitter);
            } catch (Exception e) {
                log.error("resume workflow execute error", e);
                try {
                    String errorMessage = e.getMessage() == null ? "resume workflow execute error" : e.getMessage();
                    AutoAgentExecuteResultEntity errorResult = AutoAgentExecuteResultEntity.createErrorResult(
                            errorMessage,
                            ExecutionErrorCode.SYSTEM_ERROR.getCode(),
                            "ROOT",
                            false,
                            false,
                            executeCommandEntity.getSessionId()
                    );
                    emitter.safeSend("data: " + com.alibaba.fastjson.JSON.toJSONString(errorResult) + "\n\n");
                } catch (Exception sendEx) {
                    log.error("resume workflow send error", sendEx);
                }
            } finally {
                try {
                    emitter.completeSafely();
                } catch (Exception completeEx) {
                    log.error("resume workflow complete error", completeEx);
                }
            }
        });
        return emitter;
    }

    private ResumeInterviewDetailResponseDTO.RoundItem toRoundItem(ResumeInterviewRoundEntity round) {
        return ResumeInterviewDetailResponseDTO.RoundItem.builder()
                .roundNo(round.getRoundNo())
                .questionContent(round.getQuestionContent())
                .answerContent(round.getAnswerContent())
                .feedbackContent(round.getFeedbackContent())
                .strengths(round.getStrengths())
                .weaknesses(round.getWeaknesses())
                .resumeEvidence(round.getResumeEvidence())
                .followUpIntent(round.getFollowUpIntent())
                .nextQuestion(round.getNextQuestion())
                .score(round.getScore())
                .finished(round.getFinished())
                .status(round.getStatus())
                .build();
    }

    private ResumeInterviewDetailResponseDTO toInterviewDetail(ResumeInterviewDetailEntity detail) {
        return ResumeInterviewDetailResponseDTO.builder()
                .interviewSessionId(detail.getInterviewSessionId())
                .resumeId(detail.getResumeId())
                .knowledgeSpaceId(detail.getKnowledgeSpaceId())
                .sessionCode(detail.getSessionCode())
                .currentRound(detail.getCurrentRound())
                .totalRounds(detail.getTotalRounds())
                .status(detail.getStatus())
                .openingQuestions(detail.getOpeningQuestions())
                .finalReport(detail.getFinalReport())
                .rounds(detail.getRounds() == null ? null : detail.getRounds().stream().map(this::toRoundItem).collect(Collectors.toList()))
                .build();
    }

    private ResumeProfileItemDTO toProfileItem(ResumeUploadResultEntity profile) {
        return ResumeProfileItemDTO.builder()
                .resumeId(profile.getResumeId())
                .knowledgeSpaceId(profile.getKnowledgeSpaceId())
                .knowledgeTag(profile.getKnowledgeTag())
                .fileName(profile.getFileName())
                .chunkCount(profile.getChunkCount())
                .createTime(profile.getCreateTime())
                .updateTime(profile.getUpdateTime())
                .build();
    }

    private ResumeEvaluationTaskResponseDTO toEvaluationTask(ResumeEvaluationTaskEntity task) {
        return ResumeEvaluationTaskResponseDTO.builder()
                .taskId(task.getTaskId())
                .resumeId(task.getResumeId())
                .knowledgeSpaceId(task.getKnowledgeSpaceId())
                .sessionId(task.getSessionId())
                .question(task.getQuestion())
                .status(task.getStatus())
                .report(task.getReport())
                .traceId(task.getTraceId())
                .errorMessage(task.getErrorMessage())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
    }
}
