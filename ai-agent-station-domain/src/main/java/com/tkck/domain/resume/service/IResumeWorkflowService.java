package com.tkck.domain.resume.service;

import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;
import com.tkck.domain.resume.model.entity.ResumeInterviewStartEntity;
import com.tkck.domain.resume.model.entity.ResumeUploadResultEntity;
import org.springframework.web.multipart.MultipartFile;

public interface IResumeWorkflowService {

    ResumeUploadResultEntity uploadResume(MultipartFile file) throws Exception;

    ExecuteCommandEntity buildResumeEvaluationCommand(Long resumeId, Long knowledgeSpaceId, String question, String sessionId, Integer maxStep);

    ResumeInterviewStartEntity startInterview(Long resumeId, Long knowledgeSpaceId) throws Exception;

    ExecuteCommandEntity buildInterviewAnswerCommand(Long interviewSessionId, Integer roundNo, String answer, String sessionId, Integer maxStep) throws Exception;
}
