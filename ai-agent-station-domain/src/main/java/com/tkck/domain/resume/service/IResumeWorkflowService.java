package com.tkck.domain.resume.service;

import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.agent.service.execute.auto.step.factory.DefaultAutoAgentExecuteStrategyFactory;
import com.tkck.domain.resume.model.entity.ResumeInterviewDetailEntity;
import com.tkck.domain.resume.model.entity.ResumeInterviewStartEntity;
import com.tkck.domain.resume.model.entity.ResumeUploadResultEntity;
import org.springframework.web.multipart.MultipartFile;

public interface IResumeWorkflowService {

    ResumeUploadResultEntity uploadResume(MultipartFile file) throws Exception;

    ExecuteCommandEntity buildResumeEvaluationCommand(Long resumeId, Long knowledgeSpaceId, String question, String sessionId, Integer maxStep);

    ResumeInterviewStartEntity startInterview(Long resumeId, Long knowledgeSpaceId) throws Exception;

    ExecuteCommandEntity buildInterviewAnswerCommand(Long interviewSessionId, Integer roundNo, String answer, String sessionId, Integer maxStep) throws Exception;

    void persistInterviewRoundResult(ExecuteCommandEntity executeCommandEntity,
                                     DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception;

    ResumeInterviewDetailEntity queryInterviewDetail(Long interviewSessionId);
}
