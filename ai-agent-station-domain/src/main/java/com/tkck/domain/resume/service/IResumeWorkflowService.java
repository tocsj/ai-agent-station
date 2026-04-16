package com.tkck.domain.resume.service;

import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.service.execute.auto.step.factory.DefaultAutoAgentExecuteStrategyFactory;
import com.tkck.domain.resume.model.entity.ResumeEvaluationTaskEntity;
import com.tkck.domain.resume.model.entity.ResumeInterviewDetailEntity;
import com.tkck.domain.resume.model.entity.ResumeInterviewStartEntity;
import com.tkck.domain.resume.model.entity.ResumeUploadResultEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IResumeWorkflowService {

    ResumeUploadResultEntity uploadResume(MultipartFile file) throws Exception;

    List<ResumeUploadResultEntity> queryRecentResumes(Integer limit);

    ExecuteCommandEntity buildResumeEvaluationCommand(Long resumeId, Long knowledgeSpaceId, String question, String sessionId, Integer maxStep);

    ResumeEvaluationTaskEntity queryActiveEvaluationTask();

    List<ResumeEvaluationTaskEntity> queryRecentEvaluationTasks(Integer limit);

    void persistResumeEvaluationResult(ExecuteCommandEntity executeCommandEntity,
                                       DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                       String status,
                                       String errorMessage) throws Exception;

    ResumeInterviewStartEntity startInterview(Long resumeId, Long knowledgeSpaceId) throws Exception;

    ExecuteCommandEntity buildInterviewAnswerCommand(Long interviewSessionId, Integer roundNo, String answer, String sessionId, Integer maxStep) throws Exception;

    void persistInterviewRoundResult(ExecuteCommandEntity executeCommandEntity,
                                     DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception;

    ResumeInterviewDetailEntity queryInterviewDetail(Long interviewSessionId);

    ResumeInterviewDetailEntity queryActiveInterviewDetail();
}
