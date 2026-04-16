package com.tkck.test.resume;

import com.tkck.api.dto.ResumeEvaluationTaskResponseDTO;
import com.tkck.api.dto.ResumeInterviewDetailResponseDTO;
import com.tkck.api.dto.ResumeProfileItemDTO;
import com.tkck.api.response.Response;
import com.tkck.domain.agent.service.execute.IExecuteStrategy;
import com.tkck.domain.resume.model.entity.ResumeEvaluationTaskEntity;
import com.tkck.domain.resume.model.entity.ResumeInterviewDetailEntity;
import com.tkck.domain.resume.model.entity.ResumeInterviewRoundEntity;
import com.tkck.domain.resume.model.entity.ResumeUploadResultEntity;
import com.tkck.domain.resume.service.IResumeWorkflowService;
import com.tkck.trigger.http.ResumeWorkflowController;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResumeWorkflowControllerContractTest {

    @Test
    public void shouldQueryResumeRestoreEndpoints() {
        IResumeWorkflowService resumeWorkflowService = mock(IResumeWorkflowService.class);
        ResumeWorkflowController controller = new ResumeWorkflowController();
        ReflectionTestUtils.setField(controller, "resumeWorkflowService", resumeWorkflowService);
        ReflectionTestUtils.setField(controller, "autoAgentExecuteStrategy", mock(IExecuteStrategy.class));
        ReflectionTestUtils.setField(controller, "threadPoolExecutor", Executors.newFixedThreadPool(1));

        when(resumeWorkflowService.queryRecentResumes(20)).thenReturn(List.of(ResumeUploadResultEntity.builder()
                .resumeId(7L)
                .knowledgeSpaceId(8L)
                .knowledgeTag("resume")
                .fileName("resume.pdf")
                .chunkCount(3)
                .build()));
        when(resumeWorkflowService.queryActiveEvaluationTask()).thenReturn(ResumeEvaluationTaskEntity.builder()
                .taskId(31L)
                .resumeId(7L)
                .knowledgeSpaceId(8L)
                .sessionId("resume-eval-8")
                .status("SUCCESS")
                .report("report")
                .build());
        when(resumeWorkflowService.queryRecentEvaluationTasks(20)).thenReturn(List.of(ResumeEvaluationTaskEntity.builder()
                .taskId(31L)
                .status("SUCCESS")
                .build()));
        when(resumeWorkflowService.queryActiveInterviewDetail()).thenReturn(ResumeInterviewDetailEntity.builder()
                .interviewSessionId(12L)
                .resumeId(7L)
                .knowledgeSpaceId(8L)
                .sessionCode("interview-x")
                .currentRound(1)
                .totalRounds(3)
                .status("STARTED")
                .rounds(List.of(ResumeInterviewRoundEntity.builder()
                        .roundNo(1)
                        .questionContent("question")
                        .status("ASKED")
                        .build()))
                .build());

        Response<List<ResumeProfileItemDTO>> profiles = controller.recentProfiles(20);
        Response<ResumeEvaluationTaskResponseDTO> activeEvaluation = controller.activeEvaluation();
        Response<List<ResumeEvaluationTaskResponseDTO>> recentEvaluations = controller.recentEvaluations(20);
        Response<ResumeInterviewDetailResponseDTO> activeInterview = controller.activeInterview();

        Assert.assertEquals(Long.valueOf(7L), profiles.getData().get(0).getResumeId());
        Assert.assertEquals(Long.valueOf(31L), activeEvaluation.getData().getTaskId());
        Assert.assertEquals("report", activeEvaluation.getData().getReport());
        Assert.assertEquals(1, recentEvaluations.getData().size());
        Assert.assertEquals(Long.valueOf(12L), activeInterview.getData().getInterviewSessionId());
        Assert.assertEquals(1, activeInterview.getData().getRounds().size());
    }
}
