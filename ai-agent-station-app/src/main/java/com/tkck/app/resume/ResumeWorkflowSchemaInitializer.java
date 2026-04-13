package com.tkck.app.resume;

import com.tkck.config.AiAgentConfig;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ResumeWorkflowSchemaInitializer implements InitializingBean {

    @Resource(name = "mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbcTemplate;

    @Resource(name = "pgVectorJdbcTemplate")
    private JdbcTemplate pgVectorJdbcTemplate;

    @Override
    public void afterPropertiesSet() {
        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS resume_profile (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    file_name VARCHAR(255) NOT NULL,
                    file_hash VARCHAR(64) DEFAULT NULL,
                    raw_text LONGTEXT,
                    status TINYINT DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);

        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS resume_knowledge_space (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    resume_id BIGINT NOT NULL,
                    knowledge_tag VARCHAR(64) NOT NULL,
                    vector_table VARCHAR(64) NOT NULL,
                    chunk_count INT DEFAULT 0,
                    status TINYINT DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);

        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS resume_interview_session (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    resume_id BIGINT NOT NULL,
                    knowledge_space_id BIGINT NOT NULL,
                    session_code VARCHAR(64) NOT NULL,
                    opening_questions TEXT,
                    current_round INT DEFAULT 1,
                    status VARCHAR(32) DEFAULT 'INIT',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);

        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS resume_interview_round (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    interview_session_id BIGINT NOT NULL,
                    round_no INT NOT NULL,
                    question_content TEXT,
                    answer_content TEXT,
                    feedback_content TEXT,
                    next_question TEXT,
                    status VARCHAR(32) DEFAULT 'INIT',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);

        pgVectorJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        pgVectorJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS hstore");
        pgVectorJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto");
        pgVectorJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS public.resume_vector_store (
                    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                    content text,
                    metadata jsonb,
                    embedding vector(1024)
                )
                """);
        pgVectorJdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS resume_vector_store_index
                ON public.resume_vector_store USING hnsw (embedding vector_cosine_ops)
                """);
        mysqlJdbcTemplate.execute("""
                UPDATE resume_knowledge_space
                SET vector_table = '%s', update_time = NOW()
                WHERE vector_table IS NULL OR vector_table <> '%s'
                """.formatted(AiAgentConfig.RESUME_VECTOR_TABLE, AiAgentConfig.RESUME_VECTOR_TABLE));
    }
}
