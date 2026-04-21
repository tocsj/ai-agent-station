package com.tkck.infrastructure.adapter.repository;

import com.tkck.domain.resume.adapter.repository.IResumeWorkflowRepository;
import com.tkck.domain.resume.model.entity.ResumeEvaluationTaskEntity;
import com.tkck.domain.resume.model.entity.ResumeUploadResultEntity;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Repository
public class ResumeWorkflowRepository implements IResumeWorkflowRepository {

    @Resource(name = "mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbcTemplate;

    @Override
    public Long insertResumeProfile(String fileName, String fileHash, String rawText) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        mysqlJdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO resume_profile (file_name, file_hash, raw_text, status) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, fileName);
            ps.setString(2, fileHash);
            ps.setString(3, rawText);
            ps.setInt(4, 1);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Override
    public Long insertKnowledgeSpace(Long resumeId, String knowledgeTag, String vectorTable) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        mysqlJdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO resume_knowledge_space (resume_id, knowledge_tag, vector_table, chunk_count, status) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, resumeId);
            ps.setString(2, knowledgeTag);
            ps.setString(3, vectorTable);
            ps.setInt(4, 0);
            ps.setInt(5, 1);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Override
    public void updateKnowledgeSpaceChunkCount(Long knowledgeSpaceId, int chunkCount) {
        mysqlJdbcTemplate.update(
                "UPDATE resume_knowledge_space SET chunk_count = ?, update_time = NOW() WHERE id = ?",
                chunkCount, knowledgeSpaceId);
    }

    @Override
    public List<ResumeUploadResultEntity> queryRecentResumes(int limit) {
        List<Map<String, Object>> rows = mysqlJdbcTemplate.queryForList("""
                SELECT p.id AS resume_id, p.file_name, p.create_time, p.update_time,
                       k.id AS knowledge_space_id, k.knowledge_tag, k.chunk_count
                FROM resume_profile p
                LEFT JOIN resume_knowledge_space k ON p.id = k.resume_id AND k.status = 1
                WHERE p.status = 1
                ORDER BY p.update_time DESC, p.id DESC
                LIMIT ?
                """, limit);
        return rows.stream().map(this::toResumeUploadResult).toList();
    }

    @Override
    public String queryResumeText(Long resumeId) {
        return mysqlJdbcTemplate.queryForObject(
                "SELECT raw_text FROM resume_profile WHERE id = ?",
                String.class,
                resumeId);
    }

    @Override
    public Long insertResumeEvaluationTask(Long resumeId, Long knowledgeSpaceId, String sessionId, String question) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        mysqlJdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO resume_evaluation_task (resume_id, knowledge_space_id, session_id, question, status) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, resumeId);
            ps.setLong(2, knowledgeSpaceId);
            ps.setString(3, sessionId);
            ps.setString(4, question);
            ps.setString(5, "RUNNING");
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Override
    public ResumeEvaluationTaskEntity queryActiveEvaluationTask() {
        List<Map<String, Object>> rows = mysqlJdbcTemplate.queryForList("""
                SELECT * FROM resume_evaluation_task
                ORDER BY update_time DESC, id DESC
                LIMIT 1
                """);
        return rows.isEmpty() ? null : toResumeEvaluationTask(rows.get(0));
    }

    @Override
    public List<ResumeEvaluationTaskEntity> queryRecentEvaluationTasks(int limit) {
        List<Map<String, Object>> rows = mysqlJdbcTemplate.queryForList("""
                SELECT * FROM resume_evaluation_task
                ORDER BY update_time DESC, id DESC
                LIMIT ?
                """, limit);
        return rows.stream().map(this::toResumeEvaluationTask).toList();
    }

    @Override
    public void updateResumeEvaluationTask(Long taskId, String status, String report, String traceId, String errorMessage) {
        mysqlJdbcTemplate.update("""
                UPDATE resume_evaluation_task
                SET status = ?, report = ?, trace_id = ?, error_message = ?, update_time = NOW()
                WHERE id = ?
                """, status, report, traceId, errorMessage, taskId);
    }

    @Override
    public Long insertInterviewSession(Long resumeId,
                                       Long knowledgeSpaceId,
                                       String sessionCode,
                                       String openingQuestions,
                                       int currentRound,
                                       int totalRounds,
                                       String status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        mysqlJdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO resume_interview_session (resume_id, knowledge_space_id, session_code, opening_questions, current_round, total_rounds, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, resumeId);
            ps.setLong(2, knowledgeSpaceId);
            ps.setString(3, sessionCode);
            ps.setString(4, openingQuestions);
            ps.setInt(5, currentRound);
            ps.setInt(6, totalRounds);
            ps.setString(7, status);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Override
    public void insertInterviewRound(Long interviewSessionId, int roundNo, String questionContent, String status) {
        mysqlJdbcTemplate.update(
                "INSERT INTO resume_interview_round (interview_session_id, round_no, question_content, status) VALUES (?, ?, ?, ?)",
                interviewSessionId, roundNo, questionContent, status);
    }

    @Override
    public Map<String, Object> queryInterviewSessionForAnswer(Long interviewSessionId) {
        return mysqlJdbcTemplate.queryForMap(
                "SELECT id, knowledge_space_id, current_round, total_rounds, status FROM resume_interview_session WHERE id = ?",
                interviewSessionId);
    }

    @Override
    public List<String> queryInterviewRoundQuestions(Long interviewSessionId, int roundNo) {
        return mysqlJdbcTemplate.queryForList(
                "SELECT question_content FROM resume_interview_round WHERE interview_session_id = ? AND round_no = ?",
                String.class, interviewSessionId, roundNo);
    }

    @Override
    public void updateInterviewRoundAnswer(Long interviewSessionId, int roundNo, String answer, String status) {
        mysqlJdbcTemplate.update(
                "UPDATE resume_interview_round SET answer_content = ?, status = ?, update_time = NOW() WHERE interview_session_id = ? AND round_no = ?",
                answer, status, interviewSessionId, roundNo);
    }

    @Override
    public void updateInterviewSessionStatus(Long interviewSessionId, String status) {
        mysqlJdbcTemplate.update(
                "UPDATE resume_interview_session SET status = ?, update_time = NOW() WHERE id = ?",
                status, interviewSessionId);
    }

    @Override
    public void updateInterviewRoundEvaluation(Long interviewSessionId,
                                               int roundNo,
                                               String feedbackContent,
                                               String nextQuestion,
                                               String score,
                                               String status) {
        mysqlJdbcTemplate.update(
                "UPDATE resume_interview_round SET feedback_content = ?, next_question = ?, score = ?, status = ?, update_time = NOW() WHERE interview_session_id = ? AND round_no = ?",
                feedbackContent, nextQuestion, score, status, interviewSessionId, roundNo);
    }

    @Override
    public boolean existsInterviewRound(Long interviewSessionId, int roundNo) {
        Integer exists = mysqlJdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM resume_interview_round WHERE interview_session_id = ? AND round_no = ?",
                Integer.class, interviewSessionId, roundNo);
        return exists != null && exists > 0;
    }

    @Override
    public void updateInterviewRoundQuestion(Long interviewSessionId, int roundNo, String questionContent, String status) {
        mysqlJdbcTemplate.update(
                "UPDATE resume_interview_round SET question_content = ?, status = ?, update_time = NOW() WHERE interview_session_id = ? AND round_no = ?",
                questionContent, status, interviewSessionId, roundNo);
    }

    @Override
    public void updateInterviewSessionProgress(Long interviewSessionId, int currentRound, String status) {
        mysqlJdbcTemplate.update(
                "UPDATE resume_interview_session SET current_round = ?, status = ?, update_time = NOW() WHERE id = ?",
                currentRound, status, interviewSessionId);
    }

    @Override
    public void finishInterviewSession(Long interviewSessionId, int currentRound, String finalReport) {
        mysqlJdbcTemplate.update(
                "UPDATE resume_interview_session SET current_round = ?, status = ?, final_report = ?, update_time = NOW() WHERE id = ?",
                currentRound, "FINISHED", finalReport, interviewSessionId);
    }

    @Override
    public Map<String, Object> queryInterviewDetailSession(Long interviewSessionId) {
        return mysqlJdbcTemplate.queryForMap(
                "SELECT id, resume_id, knowledge_space_id, session_code, opening_questions, current_round, total_rounds, status, final_report FROM resume_interview_session WHERE id = ?",
                interviewSessionId);
    }

    @Override
    public List<Map<String, Object>> queryInterviewDetailRounds(Long interviewSessionId) {
        return mysqlJdbcTemplate.queryForList(
                "SELECT round_no, question_content, answer_content, feedback_content, next_question, score, status FROM resume_interview_round WHERE interview_session_id = ? ORDER BY round_no ASC",
                interviewSessionId);
    }

    @Override
    public Long queryActiveInterviewSessionId() {
        List<Map<String, Object>> rows = mysqlJdbcTemplate.queryForList("""
                SELECT id
                FROM resume_interview_session
                WHERE status IN ('STARTED', 'IN_PROGRESS')
                ORDER BY CASE WHEN status IN ('STARTED', 'IN_PROGRESS') THEN 0 ELSE 1 END,
                         update_time DESC, id DESC
                LIMIT 1
                """);
        return rows.isEmpty() ? null : ((Number) rows.get(0).get("id")).longValue();
    }

    private ResumeUploadResultEntity toResumeUploadResult(Map<String, Object> row) {
        return ResumeUploadResultEntity.builder()
                .resumeId(row.get("resume_id") == null ? null : ((Number) row.get("resume_id")).longValue())
                .knowledgeSpaceId(row.get("knowledge_space_id") == null ? null : ((Number) row.get("knowledge_space_id")).longValue())
                .knowledgeTag((String) row.get("knowledge_tag"))
                .fileName((String) row.get("file_name"))
                .chunkCount(row.get("chunk_count") == null ? 0 : ((Number) row.get("chunk_count")).intValue())
                .createTime(row.get("create_time") == null ? null : String.valueOf(row.get("create_time")))
                .updateTime(row.get("update_time") == null ? null : String.valueOf(row.get("update_time")))
                .build();
    }

    private ResumeEvaluationTaskEntity toResumeEvaluationTask(Map<String, Object> row) {
        return ResumeEvaluationTaskEntity.builder()
                .taskId(((Number) row.get("id")).longValue())
                .resumeId(row.get("resume_id") == null ? null : ((Number) row.get("resume_id")).longValue())
                .knowledgeSpaceId(row.get("knowledge_space_id") == null ? null : ((Number) row.get("knowledge_space_id")).longValue())
                .sessionId((String) row.get("session_id"))
                .question((String) row.get("question"))
                .status((String) row.get("status"))
                .report((String) row.get("report"))
                .traceId((String) row.get("trace_id"))
                .errorMessage((String) row.get("error_message"))
                .createTime(row.get("create_time") == null ? null : String.valueOf(row.get("create_time")))
                .updateTime(row.get("update_time") == null ? null : String.valueOf(row.get("update_time")))
                .build();
    }
}
