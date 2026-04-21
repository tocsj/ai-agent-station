package com.tkck.domain.resume.adapter.repository;

import com.tkck.domain.resume.model.entity.ResumeEvaluationTaskEntity;
import com.tkck.domain.resume.model.entity.ResumeUploadResultEntity;

import java.util.List;
import java.util.Map;

public interface IResumeWorkflowRepository {

    Long insertResumeProfile(String fileName, String fileHash, String rawText);

    Long insertKnowledgeSpace(Long resumeId, String knowledgeTag, String vectorTable);

    void updateKnowledgeSpaceChunkCount(Long knowledgeSpaceId, int chunkCount);

    List<ResumeUploadResultEntity> queryRecentResumes(int limit);

    String queryResumeText(Long resumeId);

    Long insertResumeEvaluationTask(Long resumeId, Long knowledgeSpaceId, String sessionId, String question);

    ResumeEvaluationTaskEntity queryActiveEvaluationTask();

    List<ResumeEvaluationTaskEntity> queryRecentEvaluationTasks(int limit);

    void updateResumeEvaluationTask(Long taskId, String status, String report, String traceId, String errorMessage);

    Long insertInterviewSession(Long resumeId,
                                Long knowledgeSpaceId,
                                String sessionCode,
                                String openingQuestions,
                                int currentRound,
                                int totalRounds,
                                String status);

    void insertInterviewRound(Long interviewSessionId, int roundNo, String questionContent, String status);

    Map<String, Object> queryInterviewSessionForAnswer(Long interviewSessionId);

    List<String> queryInterviewRoundQuestions(Long interviewSessionId, int roundNo);

    void updateInterviewRoundAnswer(Long interviewSessionId, int roundNo, String answer, String status);

    void updateInterviewSessionStatus(Long interviewSessionId, String status);

    void updateInterviewRoundEvaluation(Long interviewSessionId,
                                        int roundNo,
                                        String feedbackContent,
                                        String nextQuestion,
                                        String score,
                                        String status);

    boolean existsInterviewRound(Long interviewSessionId, int roundNo);

    void updateInterviewRoundQuestion(Long interviewSessionId, int roundNo, String questionContent, String status);

    void updateInterviewSessionProgress(Long interviewSessionId, int currentRound, String status);

    void finishInterviewSession(Long interviewSessionId, int currentRound, String finalReport);

    Map<String, Object> queryInterviewDetailSession(Long interviewSessionId);

    List<Map<String, Object>> queryInterviewDetailRounds(Long interviewSessionId);

    Long queryActiveInterviewSessionId();
}
