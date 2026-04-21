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
                CREATE TABLE IF NOT EXISTS ai_knowledge_space (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    space_id VARCHAR(64) NOT NULL UNIQUE,
                    space_name VARCHAR(128) NOT NULL,
                    space_type VARCHAR(32) NOT NULL,
                    description VARCHAR(512) DEFAULT NULL,
                    status TINYINT DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);

        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_knowledge_document (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    doc_id VARCHAR(64) NOT NULL UNIQUE,
                    space_id VARCHAR(64) NOT NULL,
                    file_name VARCHAR(255) NOT NULL,
                    file_type VARCHAR(32) NOT NULL,
                    file_size BIGINT DEFAULT 0,
                    parse_status VARCHAR(32) DEFAULT 'PENDING',
                    chunk_count INT DEFAULT 0,
                    vector_status VARCHAR(32) DEFAULT 'PENDING',
                    status TINYINT DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);

        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    chunk_id VARCHAR(64) NOT NULL UNIQUE,
                    doc_id VARCHAR(64) NOT NULL,
                    space_id VARCHAR(64) NOT NULL,
                    chunk_index INT NOT NULL,
                    chunk_text LONGTEXT NOT NULL,
                    metadata_json JSON DEFAULT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);

        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS document_task_record (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    workspace_id VARCHAR(64) NOT NULL,
                    doc_id VARCHAR(64) DEFAULT NULL,
                    mode VARCHAR(32) NOT NULL,
                    question TEXT,
                    answer LONGTEXT,
                    rewritten_query TEXT,
                    retrieval_scope VARCHAR(512) DEFAULT NULL,
                    final_context LONGTEXT,
                    retrieved_chunks_json LONGTEXT,
                    retrieved_chunk_details_json LONGTEXT,
                    status VARCHAR(32) NOT NULL,
                    error_message VARCHAR(512) DEFAULT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_document_task_workspace_time (workspace_id, create_time),
                    INDEX idx_document_task_mode_time (mode, create_time)
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
                CREATE TABLE IF NOT EXISTS resume_evaluation_task (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    resume_id BIGINT NOT NULL,
                    knowledge_space_id BIGINT NOT NULL,
                    session_id VARCHAR(128) NOT NULL,
                    question TEXT,
                    status VARCHAR(32) NOT NULL,
                    report LONGTEXT,
                    trace_id VARCHAR(64) DEFAULT NULL,
                    error_message VARCHAR(512) DEFAULT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_resume_eval_resume_time (resume_id, create_time),
                    INDEX idx_resume_eval_status_time (status, create_time)
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
        addColumnIfMissing("resume_interview_session", "total_rounds", "INT DEFAULT 3");
        addColumnIfMissing("resume_interview_session", "final_report", "LONGTEXT");

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
        addColumnIfMissing("resume_interview_round", "score", "VARCHAR(64)");
        mysqlJdbcTemplate.execute("ALTER TABLE resume_interview_round MODIFY COLUMN score VARCHAR(255) NULL");

        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS content_task (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    task_code VARCHAR(64) NOT NULL UNIQUE,
                    execution_mode VARCHAR(32) NOT NULL,
                    topic VARCHAR(255) NOT NULL,
                    platform VARCHAR(64) NOT NULL,
                    style VARCHAR(64) DEFAULT NULL,
                    keywords VARCHAR(512) DEFAULT NULL,
                    channel VARCHAR(64) NOT NULL,
                    status VARCHAR(32) DEFAULT 'CREATED',
                    current_step VARCHAR(64) DEFAULT NULL,
                    title VARCHAR(255) DEFAULT NULL,
                    outline_text LONGTEXT,
                    draft_content LONGTEXT,
                    final_content LONGTEXT,
                    compliance_result LONGTEXT,
                    publish_status VARCHAR(64) DEFAULT NULL,
                    publish_external_id VARCHAR(128) DEFAULT NULL,
                    publish_external_url VARCHAR(512) DEFAULT NULL,
                    summary_text LONGTEXT,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);

        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS content_task_step (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    task_id BIGINT NOT NULL,
                    step_no INT NOT NULL,
                    step_name VARCHAR(64) NOT NULL,
                    step_status VARCHAR(32) NOT NULL,
                    output_text LONGTEXT,
                    metadata_json JSON DEFAULT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);

        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS content_publish_channel_config (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    channel_code VARCHAR(64) NOT NULL UNIQUE,
                    channel_name VARCHAR(128) NOT NULL,
                    auth_type VARCHAR(32) NOT NULL,
                    credential_json JSON DEFAULT NULL,
                    verify_status VARCHAR(32) DEFAULT 'UNCONFIGURED',
                    verify_message VARCHAR(255) DEFAULT NULL,
                    status TINYINT DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);

        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS content_publish_record (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    task_id BIGINT NOT NULL,
                    channel_code VARCHAR(64) NOT NULL,
                    action VARCHAR(64) NOT NULL,
                    request_snapshot LONGTEXT,
                    response_snapshot LONGTEXT,
                    status VARCHAR(64) DEFAULT NULL,
                    external_id VARCHAR(128) DEFAULT NULL,
                    external_url VARCHAR(512) DEFAULT NULL,
                    error_message VARCHAR(512) DEFAULT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);

        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS audit_event (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    event_id VARCHAR(64) NOT NULL UNIQUE,
                    event_type VARCHAR(64) NOT NULL,
                    biz_type VARCHAR(64) NOT NULL,
                    biz_id VARCHAR(64) DEFAULT NULL,
                    session_id VARCHAR(128) DEFAULT NULL,
                    execution_mode VARCHAR(64) DEFAULT NULL,
                    operator_id VARCHAR(64) DEFAULT 'global',
                    operator_name VARCHAR(128) DEFAULT '全局账号',
                    request_uri VARCHAR(255) DEFAULT NULL,
                    request_method VARCHAR(16) DEFAULT NULL,
                    status VARCHAR(32) NOT NULL,
                    error_code VARCHAR(64) DEFAULT NULL,
                    error_message VARCHAR(512) DEFAULT NULL,
                    location VARCHAR(255) DEFAULT NULL,
                    metadata_json JSON DEFAULT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_audit_event_biz (biz_type, biz_id),
                    INDEX idx_audit_event_type_status (event_type, status),
                    INDEX idx_audit_event_create_time (create_time)
                )
                """);

        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS agent_execution_metric (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    trace_id VARCHAR(64) NOT NULL UNIQUE,
                    task_type VARCHAR(64) NOT NULL,
                    task_sub_type VARCHAR(64) DEFAULT NULL,
                    task_id VARCHAR(64) DEFAULT NULL,
                    session_id VARCHAR(128) DEFAULT NULL,
                    execution_mode VARCHAR(64) DEFAULT NULL,
                    status VARCHAR(32) NOT NULL,
                    total_duration_ms BIGINT DEFAULT 0,
                    step_count INT DEFAULT 0,
                    success_step_count INT DEFAULT 0,
                    failed_step_count INT DEFAULT 0,
                    timeout_count INT DEFAULT 0,
                    retry_count INT DEFAULT 0,
                    degraded_count INT DEFAULT 0,
                    model_calls INT DEFAULT 0,
                    prompt_tokens BIGINT DEFAULT 0,
                    completion_tokens BIGINT DEFAULT 0,
                    total_tokens BIGINT DEFAULT 0,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    finish_time DATETIME DEFAULT NULL,
                    INDEX idx_execution_metric_task (task_type, task_id),
                    INDEX idx_execution_metric_status_time (status, create_time)
                )
                """);

        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS agent_step_metric (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    trace_id VARCHAR(64) NOT NULL,
                    task_id VARCHAR(64) DEFAULT NULL,
                    session_id VARCHAR(128) DEFAULT NULL,
                    step_no INT NOT NULL,
                    step_name VARCHAR(64) NOT NULL,
                    stage VARCHAR(64) NOT NULL,
                    client_id VARCHAR(64) DEFAULT NULL,
                    model_code VARCHAR(64) DEFAULT NULL,
                    status VARCHAR(32) NOT NULL,
                    duration_ms BIGINT DEFAULT 0,
                    retry_count INT DEFAULT 0,
                    timeout_flag TINYINT DEFAULT 0,
                    degraded_flag TINYINT DEFAULT 0,
                    error_code VARCHAR(64) DEFAULT NULL,
                    error_message VARCHAR(512) DEFAULT NULL,
                    location VARCHAR(255) DEFAULT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_step_metric_trace (trace_id),
                    INDEX idx_step_metric_name_time (step_name, create_time),
                    INDEX idx_step_metric_status_time (status, create_time)
                )
                """);
        addColumnIfMissing("agent_execution_metric", "task_sub_type", "VARCHAR(64) DEFAULT NULL");
        addColumnIfMissing("agent_execution_metric", "prompt_tokens", "BIGINT DEFAULT 0");
        addColumnIfMissing("agent_execution_metric", "completion_tokens", "BIGINT DEFAULT 0");
        addColumnIfMissing("agent_execution_metric", "total_tokens", "BIGINT DEFAULT 0");

        mysqlJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS agent_llm_call_metric (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    call_id VARCHAR(64) NOT NULL UNIQUE,
                    trace_id VARCHAR(64) NOT NULL,
                    task_type VARCHAR(64) NOT NULL,
                    task_sub_type VARCHAR(64) DEFAULT NULL,
                    task_id VARCHAR(64) DEFAULT NULL,
                    session_id VARCHAR(128) DEFAULT NULL,
                    step_name VARCHAR(64) NOT NULL,
                    stage VARCHAR(64) DEFAULT NULL,
                    client_id VARCHAR(64) DEFAULT NULL,
                    model_code VARCHAR(64) DEFAULT NULL,
                    status VARCHAR(32) NOT NULL,
                    duration_ms BIGINT DEFAULT 0,
                    prompt_tokens BIGINT DEFAULT 0,
                    completion_tokens BIGINT DEFAULT 0,
                    total_tokens BIGINT DEFAULT 0,
                    error_code VARCHAR(64) DEFAULT NULL,
                    error_message VARCHAR(512) DEFAULT NULL,
                    location VARCHAR(255) DEFAULT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_llm_call_trace (trace_id),
                    INDEX idx_llm_call_task_time (task_type, create_time),
                    INDEX idx_llm_call_model_time (client_id, model_code, create_time)
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

        pgVectorJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS public.job_standard_profile (
                    id BIGSERIAL PRIMARY KEY,
                    job_code VARCHAR(64) NOT NULL UNIQUE,
                    job_name VARCHAR(128) NOT NULL,
                    job_family VARCHAR(64),
                    job_level VARCHAR(64),
                    description TEXT,
                    status SMALLINT DEFAULT 1,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        pgVectorJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS public.job_standard_item (
                    id BIGSERIAL PRIMARY KEY,
                    profile_id BIGINT NOT NULL,
                    job_code VARCHAR(64) NOT NULL,
                    category VARCHAR(64) NOT NULL,
                    skill_key VARCHAR(64) NOT NULL,
                    skill_name VARCHAR(128) NOT NULL,
                    importance VARCHAR(16) NOT NULL,
                    expected_level VARCHAR(16) NOT NULL,
                    standard_summary TEXT,
                    detailed_requirement TEXT,
                    scoring_points TEXT,
                    risk_signals TEXT,
                    status SMALLINT DEFAULT 1,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        pgVectorJdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS job_standard_item_job_code_idx
                ON public.job_standard_item(job_code)
                """);
        pgVectorJdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS job_standard_item_skill_key_idx
                ON public.job_standard_item(skill_key)
                """);
        pgVectorJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS public.job_standard_vector_store (
                    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                    content text,
                    metadata jsonb,
                    embedding vector(1024)
                )
                """);
        pgVectorJdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS job_standard_vector_store_index
                ON public.job_standard_vector_store USING hnsw (embedding vector_cosine_ops)
                """);

        pgVectorJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS public.document_vector_store (
                    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                    content text,
                    metadata jsonb,
                    embedding vector(1024)
                )
                """);
        pgVectorJdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS document_vector_store_index
                ON public.document_vector_store USING hnsw (embedding vector_cosine_ops)
                """);

        syncAutoRuntimeClientModel();
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnDefinition) {
        Integer count = mysqlJdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                          AND COLUMN_NAME = ?
                        """,
                Integer.class,
                tableName,
                columnName);
        if (count == null || count == 0) {
            mysqlJdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    /**
     * 保留原有“数据库动态装配 client”机制，只在启动时把四步链路的 1/4 步 client
     * 映射到保留模型方案中的 flash 模型（约定 2004 为 qwen3.5-flash）。
     */
    private void syncAutoRuntimeClientModel() {
        ensureContentPublishClients();
        ensureDocumentClients();
        ensureInterviewStep1Client();
        mysqlJdbcTemplate.update("""
                UPDATE ai_client_config
                SET target_id = '2004', update_time = NOW()
                WHERE source_type = 'client'
                  AND target_type = 'model'
                  AND source_id IN ('5101', '5104', '5204')
                  AND status = 1
                  AND target_id <> '2004'
                """);
    }

    private void ensureInterviewStep1Client() {
        ensureClientModelConfig("5201", "2008");
    }

    private void ensureDocumentClients() {
        ensureClient("5401", "文档知识助手-通用处理", "文档知识助手通用问答与摘要客户端");
        ensureClientModelConfig("5401", "2008");
    }

    private void ensureContentPublishClients() {
        ensureClient("5301", "企业内容发布-高质量生成", "企业内容发布高质量生成客户端");
        ensureClient("5302", "企业内容发布-轻量处理", "企业内容发布轻量处理客户端");
        ensureClientModelConfig("5301", "2007");
        ensureClientModelConfig("5302", "2008");
    }

    private void ensureClient(String clientId, String clientName, String description) {
        Integer count = mysqlJdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM ai_client
                        WHERE client_id = ?
                        """,
                Integer.class,
                clientId);
        if (count == null || count == 0) {
            mysqlJdbcTemplate.update("""
                            INSERT INTO ai_client (client_id, client_name, description, status, create_time, update_time)
                            VALUES (?, ?, ?, 1, NOW(), NOW())
                            """,
                    clientId, clientName, description);
        }
    }

    private void ensureClientModelConfig(String clientId, String modelId) {
        Integer count = mysqlJdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM ai_client_config
                        WHERE source_type = 'client'
                          AND source_id = ?
                          AND target_type = 'model'
                          AND status = 1
                        """,
                Integer.class,
                clientId);
        if (count == null || count == 0) {
            mysqlJdbcTemplate.update("""
                            INSERT INTO ai_client_config
                            (source_type, source_id, target_type, target_id, ext_param, status, create_time, update_time)
                            VALUES ('client', ?, 'model', ?, '""', 1, NOW(), NOW())
                            """,
                    clientId, modelId);
            return;
        }
        mysqlJdbcTemplate.update("""
                        UPDATE ai_client_config
                        SET target_id = ?, update_time = NOW()
                        WHERE source_type = 'client'
                          AND source_id = ?
                          AND target_type = 'model'
                          AND status = 1
                          AND target_id <> ?
                        """,
                modelId, clientId, modelId);
    }
}
