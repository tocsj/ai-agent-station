-- AI Agent Station MySQL initialization script
-- Contains full table structure and only base agent/client/model configuration data.
-- Runtime business data, resume data, document data, audit data and publish credentials are intentionally excluded.


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `ai-agent-station` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `ai-agent-station`;
DROP TABLE IF EXISTS `agent_execution_metric`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_execution_metric` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `trace_id` varchar(64) NOT NULL,
  `task_type` varchar(64) NOT NULL,
  `task_id` varchar(64) DEFAULT NULL,
  `session_id` varchar(128) DEFAULT NULL,
  `execution_mode` varchar(64) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `total_duration_ms` bigint DEFAULT '0',
  `step_count` int DEFAULT '0',
  `success_step_count` int DEFAULT '0',
  `failed_step_count` int DEFAULT '0',
  `timeout_count` int DEFAULT '0',
  `retry_count` int DEFAULT '0',
  `degraded_count` int DEFAULT '0',
  `model_calls` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `finish_time` datetime DEFAULT NULL,
  `task_sub_type` varchar(64) DEFAULT NULL,
  `prompt_tokens` bigint DEFAULT '0',
  `completion_tokens` bigint DEFAULT '0',
  `total_tokens` bigint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `trace_id` (`trace_id`),
  KEY `idx_execution_metric_task` (`task_type`,`task_id`),
  KEY `idx_execution_metric_status_time` (`status`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `agent_llm_call_metric`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_llm_call_metric` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `call_id` varchar(64) NOT NULL,
  `trace_id` varchar(64) NOT NULL,
  `task_type` varchar(64) NOT NULL,
  `task_sub_type` varchar(64) DEFAULT NULL,
  `task_id` varchar(64) DEFAULT NULL,
  `session_id` varchar(128) DEFAULT NULL,
  `step_name` varchar(64) NOT NULL,
  `stage` varchar(64) DEFAULT NULL,
  `client_id` varchar(64) DEFAULT NULL,
  `model_code` varchar(64) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `duration_ms` bigint DEFAULT '0',
  `prompt_tokens` bigint DEFAULT '0',
  `completion_tokens` bigint DEFAULT '0',
  `total_tokens` bigint DEFAULT '0',
  `error_code` varchar(64) DEFAULT NULL,
  `error_message` varchar(512) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `call_id` (`call_id`),
  KEY `idx_llm_call_trace` (`trace_id`),
  KEY `idx_llm_call_task_time` (`task_type`,`create_time`),
  KEY `idx_llm_call_model_time` (`client_id`,`model_code`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=212 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `agent_step_metric`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_step_metric` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `trace_id` varchar(64) NOT NULL,
  `task_id` varchar(64) DEFAULT NULL,
  `session_id` varchar(128) DEFAULT NULL,
  `step_no` int NOT NULL,
  `step_name` varchar(64) NOT NULL,
  `stage` varchar(64) NOT NULL,
  `client_id` varchar(64) DEFAULT NULL,
  `model_code` varchar(64) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `duration_ms` bigint DEFAULT '0',
  `retry_count` int DEFAULT '0',
  `timeout_flag` tinyint DEFAULT '0',
  `degraded_flag` tinyint DEFAULT '0',
  `error_code` varchar(64) DEFAULT NULL,
  `error_message` varchar(512) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_step_metric_trace` (`trace_id`),
  KEY `idx_step_metric_name_time` (`step_name`,`create_time`),
  KEY `idx_step_metric_status_time` (`status`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=144 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_agent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_agent` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` varchar(64) NOT NULL COMMENT '智能体ID',
  `agent_name` varchar(50) NOT NULL COMMENT '智能体名称',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `channel` varchar(32) DEFAULT NULL COMMENT '渠道类型(agent，chat_stream)',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_id` (`agent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI智能体配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_agent_flow_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_agent_flow_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` varchar(64) NOT NULL COMMENT '智能体ID',
  `client_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户端ID',
  `client_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户端名称',
  `client_type` varchar(64) DEFAULT NULL COMMENT '客户端类型',
  `sequence` int NOT NULL COMMENT '序列号(执行顺序)',
  `step_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '步骤提示词',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_client_seq` (`agent_id`,`client_id`,`sequence`)
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能体-客户端关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_agent_task_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_agent_task_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `task_name` varchar(64) DEFAULT NULL COMMENT '任务名称',
  `description` varchar(255) DEFAULT NULL COMMENT '任务描述',
  `cron_expression` varchar(50) NOT NULL COMMENT '时间表达式(如: 0/3 * * * * *)',
  `task_param` text COMMENT '任务入参配置(JSON格式)',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:无效,1:有效)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能体任务调度配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_client` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `client_id` varchar(64) NOT NULL COMMENT '客户端ID',
  `client_name` varchar(50) NOT NULL COMMENT '客户端名称',
  `description` varchar(1024) DEFAULT NULL COMMENT '描述',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `client_id` (`client_id`)
) ENGINE=InnoDB AUTO_INCREMENT=74 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI客户端配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_client_advisor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_client_advisor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `advisor_id` varchar(64) NOT NULL COMMENT '顾问ID',
  `advisor_name` varchar(50) NOT NULL COMMENT '顾问名称',
  `advisor_type` varchar(50) NOT NULL COMMENT '顾问类型(PromptChatMemory/RagAnswer/SimpleLoggerAdvisor等)',
  `order_num` int DEFAULT '0' COMMENT '顺序号',
  `ext_param` varchar(2048) DEFAULT NULL COMMENT '扩展参数配置，json 记录',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_advisor_id` (`advisor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='顾问配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_client_api`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_client_api` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
  `api_id` varchar(64) NOT NULL COMMENT '全局唯一配置ID',
  `base_url` varchar(255) NOT NULL COMMENT 'API基础URL',
  `api_key` varchar(255) NOT NULL COMMENT 'API密钥',
  `completions_path` varchar(255) NOT NULL COMMENT '补全API路径',
  `embeddings_path` varchar(255) NOT NULL COMMENT '嵌入API路径',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_id` (`api_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OpenAI API配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_client_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_client_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `source_type` varchar(32) NOT NULL COMMENT '源类型（model、client）',
  `source_id` varchar(64) NOT NULL COMMENT '源ID（如 chatModelId、chatClientId 等）',
  `target_type` varchar(32) NOT NULL COMMENT '目标类型（model、client）',
  `target_id` varchar(64) NOT NULL COMMENT '目标ID（如 openAiApiId、chatModelId、systemPromptId、advisorId 等）',
  `ext_param` varchar(1024) DEFAULT NULL COMMENT '扩展参数（JSON格式）',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_source_id` (`source_id`),
  KEY `idx_target_id` (`target_id`)
) ENGINE=InnoDB AUTO_INCREMENT=189 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI客户端统一关联配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_client_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_client_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
  `model_id` varchar(64) NOT NULL COMMENT '全局唯一模型ID',
  `api_id` varchar(64) NOT NULL COMMENT '关联的API配置ID',
  `model_name` varchar(64) NOT NULL COMMENT '模型名称',
  `model_type` varchar(32) NOT NULL COMMENT '模型类型：openai、deepseek、claude',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_id` (`model_id`),
  KEY `idx_api_config_id` (`api_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天模型配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_client_rag_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_client_rag_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rag_id` varchar(50) NOT NULL COMMENT '知识库ID',
  `rag_name` varchar(50) NOT NULL COMMENT '知识库名称',
  `knowledge_tag` varchar(50) NOT NULL COMMENT '知识标签',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rag_id` (`rag_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_client_system_prompt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_client_system_prompt` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `prompt_id` varchar(64) NOT NULL COMMENT '提示词ID',
  `prompt_name` varchar(50) NOT NULL COMMENT '提示词名称',
  `prompt_content` text NOT NULL COMMENT '提示词内容',
  `description` varchar(1024) DEFAULT NULL COMMENT '描述',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_prompt_id` (`prompt_id`)
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统提示词配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_client_tool_mcp`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_client_tool_mcp` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `mcp_id` varchar(64) NOT NULL COMMENT 'MCP名称',
  `mcp_name` varchar(50) NOT NULL COMMENT 'MCP名称',
  `transport_type` varchar(20) NOT NULL COMMENT '传输类型(sse/stdio)',
  `transport_config` varchar(1024) DEFAULT NULL COMMENT '传输配置(sse/stdio)',
  `request_timeout` int DEFAULT '180' COMMENT '请求超时时间(分钟)',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mcp_id` (`mcp_id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MCP客户端配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_knowledge_chunk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_chunk` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chunk_id` varchar(64) NOT NULL,
  `doc_id` varchar(64) NOT NULL,
  `space_id` varchar(64) NOT NULL,
  `chunk_index` int NOT NULL,
  `chunk_text` longtext NOT NULL,
  `metadata_json` json DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `chunk_id` (`chunk_id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_knowledge_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_document` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `doc_id` varchar(64) NOT NULL,
  `space_id` varchar(64) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_type` varchar(32) NOT NULL,
  `file_size` bigint DEFAULT '0',
  `parse_status` varchar(32) DEFAULT 'PENDING',
  `chunk_count` int DEFAULT '0',
  `vector_status` varchar(32) DEFAULT 'PENDING',
  `status` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `doc_id` (`doc_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_knowledge_space`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_space` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_id` varchar(64) NOT NULL,
  `space_name` varchar(128) NOT NULL,
  `space_type` varchar(32) NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `status` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `space_id` (`space_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `audit_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` varchar(64) NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `biz_type` varchar(64) NOT NULL,
  `biz_id` varchar(64) DEFAULT NULL,
  `session_id` varchar(128) DEFAULT NULL,
  `execution_mode` varchar(64) DEFAULT NULL,
  `operator_id` varchar(64) DEFAULT 'global',
  `operator_name` varchar(128) DEFAULT '全局账号',
  `request_uri` varchar(255) DEFAULT NULL,
  `request_method` varchar(16) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `error_code` varchar(64) DEFAULT NULL,
  `error_message` varchar(512) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `metadata_json` json DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `event_id` (`event_id`),
  KEY `idx_audit_event_biz` (`biz_type`,`biz_id`),
  KEY `idx_audit_event_type_status` (`event_type`,`status`),
  KEY `idx_audit_event_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=80 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `content_publish_channel_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_publish_channel_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel_code` varchar(64) NOT NULL,
  `channel_name` varchar(128) NOT NULL,
  `auth_type` varchar(32) NOT NULL,
  `credential_json` json DEFAULT NULL,
  `verify_status` varchar(32) DEFAULT 'UNCONFIGURED',
  `verify_message` varchar(255) DEFAULT NULL,
  `status` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `channel_code` (`channel_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `content_publish_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_publish_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `channel_code` varchar(64) NOT NULL,
  `action` varchar(64) NOT NULL,
  `request_snapshot` longtext,
  `response_snapshot` longtext,
  `status` varchar(64) DEFAULT NULL,
  `external_id` varchar(128) DEFAULT NULL,
  `external_url` varchar(512) DEFAULT NULL,
  `error_message` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `content_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_code` varchar(64) NOT NULL,
  `execution_mode` varchar(32) NOT NULL,
  `topic` varchar(255) NOT NULL,
  `platform` varchar(64) NOT NULL,
  `style` varchar(64) DEFAULT NULL,
  `keywords` varchar(512) DEFAULT NULL,
  `channel` varchar(64) NOT NULL,
  `status` varchar(32) DEFAULT 'CREATED',
  `current_step` varchar(64) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `outline_text` longtext,
  `draft_content` longtext,
  `final_content` longtext,
  `compliance_result` longtext,
  `publish_status` varchar(64) DEFAULT NULL,
  `publish_external_id` varchar(128) DEFAULT NULL,
  `publish_external_url` varchar(512) DEFAULT NULL,
  `summary_text` longtext,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `task_code` (`task_code`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `content_task_step`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_task_step` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `step_no` int NOT NULL,
  `step_name` varchar(64) NOT NULL,
  `step_status` varchar(32) NOT NULL,
  `output_text` longtext,
  `metadata_json` json DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=90 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `document_task_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document_task_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `workspace_id` varchar(64) NOT NULL,
  `doc_id` varchar(64) DEFAULT NULL,
  `mode` varchar(32) NOT NULL,
  `question` text,
  `answer` longtext,
  `rewritten_query` text,
  `retrieval_scope` varchar(512) DEFAULT NULL,
  `final_context` longtext,
  `retrieved_chunks_json` longtext,
  `retrieved_chunk_details_json` longtext,
  `status` varchar(32) NOT NULL,
  `error_message` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_document_task_workspace_time` (`workspace_id`,`create_time`),
  KEY `idx_document_task_mode_time` (`mode`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `resume_evaluation_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume_evaluation_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resume_id` bigint NOT NULL,
  `knowledge_space_id` bigint NOT NULL,
  `session_id` varchar(128) NOT NULL,
  `question` text,
  `status` varchar(32) NOT NULL,
  `report` longtext,
  `trace_id` varchar(64) DEFAULT NULL,
  `error_message` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_resume_eval_resume_time` (`resume_id`,`create_time`),
  KEY `idx_resume_eval_status_time` (`status`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `resume_interview_round`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume_interview_round` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `interview_session_id` bigint NOT NULL,
  `round_no` int NOT NULL,
  `question_content` text,
  `answer_content` text,
  `feedback_content` text,
  `next_question` text,
  `status` varchar(32) DEFAULT 'INIT',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `score` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `resume_interview_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume_interview_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resume_id` bigint NOT NULL,
  `knowledge_space_id` bigint NOT NULL,
  `session_code` varchar(64) NOT NULL,
  `opening_questions` text,
  `current_round` int DEFAULT '1',
  `status` varchar(32) DEFAULT 'INIT',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `total_rounds` int DEFAULT '3',
  `final_report` longtext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `resume_knowledge_space`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume_knowledge_space` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resume_id` bigint NOT NULL,
  `knowledge_tag` varchar(64) NOT NULL,
  `vector_table` varchar(64) NOT NULL,
  `chunk_count` int DEFAULT '0',
  `status` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `resume_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) NOT NULL,
  `file_hash` varchar(64) DEFAULT NULL,
  `raw_text` longtext,
  `status` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


-- Base configuration data

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

LOCK TABLES `ai_agent` WRITE;
/*!40000 ALTER TABLE `ai_agent` DISABLE KEYS */;
INSERT INTO `ai_agent` (`id`, `agent_id`, `agent_name`, `description`, `channel`, `status`, `create_time`, `update_time`) VALUES (21,'1001','简历评估智能体','基于候选人简历知识空间执行简历评估。','agent',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(22,'1002','模拟面试智能体','基于候选人简历知识空间执行模拟面试。','agent',1,'2026-04-13 10:12:29','2026-04-13 10:12:29');
/*!40000 ALTER TABLE `ai_agent` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `ai_agent_flow_config` WRITE;
/*!40000 ALTER TABLE `ai_agent_flow_config` DISABLE KEYS */;
INSERT INTO `ai_agent_flow_config` (`id`, `agent_id`, `client_id`, `client_name`, `client_type`, `sequence`, `step_prompt`, `create_time`) VALUES (46,'1001','5101','简历评估-任务分析','TASK_ANALYZER_CLIENT',1,'拆解简历评估任务','2026-04-13 10:12:29'),(47,'1001','5102','简历评估-执行评估','PRECISION_EXECUTOR_CLIENT',2,'执行简历评估','2026-04-13 10:12:29'),(48,'1001','5103','简历评估-质量复核','QUALITY_SUPERVISOR_CLIENT',3,'复核评估质量','2026-04-13 10:12:29'),(49,'1001','5104','简历评估-结果总结','RESPONSE_ASSISTANT',4,'生成最终简历评估报告','2026-04-13 10:12:29'),(50,'1002','5201','模拟面试-任务分析','TASK_ANALYZER_CLIENT',1,'拆解模拟面试计划','2026-04-13 10:12:29'),(51,'1002','5202','模拟面试-面试执行','PRECISION_EXECUTOR_CLIENT',2,'推进模拟面试轮次','2026-04-13 10:12:29'),(52,'1002','5203','模拟面试-质量复核','QUALITY_SUPERVISOR_CLIENT',3,'复核面试质量','2026-04-13 10:12:29'),(53,'1002','5204','模拟面试-结果总结','RESPONSE_ASSISTANT',4,'输出最终面试反馈报告','2026-04-13 10:12:29');
/*!40000 ALTER TABLE `ai_agent_flow_config` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `ai_client` WRITE;
/*!40000 ALTER TABLE `ai_client` DISABLE KEYS */;
INSERT INTO `ai_client` (`id`, `client_id`, `client_name`, `description`, `status`, `create_time`, `update_time`) VALUES (63,'5101','简历评估-任务分析','简历评估任务拆解客户端',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(64,'5102','简历评估-执行评估','简历评估执行客户端',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(65,'5103','简历评估-质量复核','简历评估质量复核客户端',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(66,'5104','简历评估-结果总结','简历评估总结客户端',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(67,'5201','模拟面试-任务分析','模拟面试任务拆解客户端',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(68,'5202','模拟面试-面试执行','模拟面试执行客户端',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(69,'5203','模拟面试-质量复核','模拟面试质量复核客户端',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(70,'5204','模拟面试-结果总结','模拟面试总结客户端',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(71,'5301','企业内容发布-高质量生成','企业内容发布高质量生成客户端',1,'2026-04-15 16:47:44','2026-04-15 16:47:44'),(72,'5302','企业内容发布-轻量处理','企业内容发布轻量处理客户端',1,'2026-04-15 16:47:44','2026-04-15 16:47:44'),(73,'5401','文档知识助手-通用处理','文档知识助手通用问答与摘要客户端',1,'2026-04-16 13:14:52','2026-04-16 13:14:52');
/*!40000 ALTER TABLE `ai_client` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `ai_client_advisor` WRITE;
/*!40000 ALTER TABLE `ai_client_advisor` DISABLE KEYS */;
INSERT INTO `ai_client_advisor` (`id`, `advisor_id`, `advisor_name`, `advisor_type`, `order_num`, `ext_param`, `status`, `create_time`, `update_time`) VALUES (9,'4001','chat-memory','ChatMemory',1,'{\"maxMessages\":200}',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(10,'4002','resume-knowledge-rag','RagAnswer',2,'{\"topK\":6,\"filterExpression\":\"knowledge == \'resume_knowledge_space\'\"}',1,'2026-04-13 10:12:29','2026-04-13 10:12:29');
/*!40000 ALTER TABLE `ai_client_advisor` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `ai_client_api` WRITE;
/*!40000 ALTER TABLE `ai_client_api` DISABLE KEYS */;
INSERT INTO `ai_client_api` (`id`, `api_id`, `base_url`, `api_key`, `completions_path`, `embeddings_path`, `status`, `create_time`, `update_time`) VALUES (1,'1001','https://free.v36.cm','REPLACE_WITH_MODEL_API_KEY','v1/chat/completions','v1/embeddings',1,'2025-06-14 12:33:22','2025-07-27 14:50:17'),(2,'1002','https://api.hunyuan.cloud.tencent.com','REPLACE_WITH_MODEL_API_KEY','v1/chat/completions','v1/embeddings',1,'2026-03-02 20:33:52','2026-03-02 20:33:52'),(3,'1003','https://dashscope.aliyuncs.com/compatible-mode/','REPLACE_WITH_MODEL_API_KEY','v1/chat/completions','v1/embeddings',1,'2026-04-12 22:31:44','2026-04-13 12:18:03');
/*!40000 ALTER TABLE `ai_client_api` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `ai_client_config` WRITE;
/*!40000 ALTER TABLE `ai_client_config` DISABLE KEYS */;
INSERT INTO `ai_client_config` (`id`, `source_type`, `source_id`, `target_type`, `target_id`, `ext_param`, `status`, `create_time`, `update_time`) VALUES (154,'client','5101','model','2004','\"\"',1,'2026-04-13 10:12:29','2026-04-13 19:37:39'),(155,'client','5101','prompt','9101','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(156,'client','5101','advisor','4001','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(157,'client','5101','advisor','4002','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(158,'client','5102','model','2003','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(159,'client','5102','prompt','9102','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(160,'client','5102','advisor','4001','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(161,'client','5102','advisor','4002','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(162,'client','5103','model','2004','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(163,'client','5103','prompt','9103','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(164,'client','5103','advisor','4001','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(165,'client','5103','advisor','4002','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(166,'client','5104','model','2004','\"\"',1,'2026-04-13 10:12:29','2026-04-13 19:37:39'),(167,'client','5104','prompt','9104','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(168,'client','5104','advisor','4001','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(169,'client','5104','advisor','4002','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(170,'client','5201','model','2008','\"\"',1,'2026-04-13 10:12:29','2026-04-16 19:38:16'),(171,'client','5201','prompt','9201','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(172,'client','5201','advisor','4001','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(173,'client','5201','advisor','4002','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(174,'client','5202','model','2003','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(175,'client','5202','prompt','9202','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(176,'client','5202','advisor','4001','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(177,'client','5202','advisor','4002','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(178,'client','5203','model','2004','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(179,'client','5203','prompt','9203','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(180,'client','5203','advisor','4001','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(181,'client','5203','advisor','4002','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(182,'client','5204','model','2004','\"\"',1,'2026-04-13 10:12:29','2026-04-13 19:37:39'),(183,'client','5204','prompt','9204','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(184,'client','5204','advisor','4001','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(185,'client','5204','advisor','4002','\"\"',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(186,'client','5301','model','2007','\"\"',1,'2026-04-15 16:47:44','2026-04-15 16:47:44'),(187,'client','5302','model','2008','\"\"',1,'2026-04-15 16:47:44','2026-04-15 16:47:44'),(188,'client','5401','model','2008','\"\"',1,'2026-04-16 13:14:52','2026-04-16 13:14:52');
/*!40000 ALTER TABLE `ai_client_config` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `ai_client_model` WRITE;
/*!40000 ALTER TABLE `ai_client_model` DISABLE KEYS */;
INSERT INTO `ai_client_model` (`id`, `model_id`, `api_id`, `model_name`, `model_type`, `status`, `create_time`, `update_time`) VALUES (1,'2001','1001','gpt-4o-mini','openai',1,'2025-06-14 12:33:47','2025-06-14 12:33:47'),(2,'2002','1002','hunyuan-turbos-latest','openai',1,'2026-03-02 20:36:08','2026-03-02 20:36:08'),(3,'2003','1003','qwen-plus-latest','openai',1,'2026-04-12 22:34:08','2026-04-13 10:12:29'),(4,'2004','1003','qwen3.5-flash-2026-02-23','openai',1,'2026-04-12 22:36:40','2026-04-16 18:38:11'),(5,'2005','1003','text-embedding-v4','openai',1,'2026-04-12 22:37:42','2026-04-13 10:12:29'),(6,'2006','1003','qwen3-rerank','openai',1,'2026-04-12 22:38:04','2026-04-13 10:12:29'),(7,'2007','1003','qwen-plus','openai',1,'2026-04-15 15:34:12','2026-04-15 15:34:14'),(8,'2008','1003','qwen-flash','openai',1,'2026-04-15 15:51:59','2026-04-15 15:52:00');
/*!40000 ALTER TABLE `ai_client_model` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `ai_client_rag_order` WRITE;
/*!40000 ALTER TABLE `ai_client_rag_order` DISABLE KEYS */;
INSERT INTO `ai_client_rag_order` (`id`, `rag_id`, `rag_name`, `knowledge_tag`, `status`, `create_time`, `update_time`) VALUES (4,'9001','Resume Knowledge Space','resume_knowledge_space',1,'2026-04-13 10:12:29','2026-04-13 10:12:29');
/*!40000 ALTER TABLE `ai_client_rag_order` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `ai_client_system_prompt` WRITE;
/*!40000 ALTER TABLE `ai_client_system_prompt` DISABLE KEYS */;
INSERT INTO `ai_client_system_prompt` (`id`, `prompt_id`, `prompt_name`, `prompt_content`, `description`, `status`, `create_time`, `update_time`) VALUES (55,'9101','简历评估-任务分析提示词','你负责把候选人的问题和简历内容拆解为评估任务。请输出：评估目标、关键信息缺口、需要重点核验的经历、后续执行步骤。要求结构清晰，避免空泛表述。','简历评估任务分析提示词。',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(56,'9102','简历评估-执行提示词','你负责执行简历评估。请结合候选人简历知识空间，输出岗位匹配度、技术深度、项目真实性信号、优势、短板和修改建议。结论必须具体，能够直接用于候选人改简历。','简历评估执行提示词。',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(57,'9103','简历评估-复核提示词','你负责质量复核。请检查评估结果是否存在证据不足、结论跳跃、缺少引用、建议不可执行等问题，并给出修正意见。你的职责不是重复总结，而是指出漏洞。','简历评估复核提示词。',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(58,'9104','简历评估-总结提示词','你负责输出最终简历评估报告。请按总体结论、优势亮点、主要风险、优化建议四个部分组织内容，要求专业、直接、可执行，适合直接给候选人查看。','简历评估总结提示词。',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(59,'9201','模拟面试-任务分析提示词','你负责生成模拟面试计划。请基于候选人简历知识空间，明确本轮面试目标、题目范围、难度分层、重点追问点和预期考察能力。','模拟面试任务分析提示词。',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(60,'9202','模拟面试-执行提示词','你负责推进模拟面试。请逐轮提出问题，给出本轮评价维度和追问策略，并根据候选人的回答动态调整难度和追问方向，让面试过程更像真实技术面试。','模拟面试执行提示词。',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(61,'9203','模拟面试-复核提示词','你负责复核模拟面试过程。请检查题目是否贴合简历、难度是否合理、追问是否有效、评价是否有依据，并指出需要修正的地方，避免形式化面试。','模拟面试复核提示词。',1,'2026-04-13 10:12:29','2026-04-13 10:12:29'),(62,'9204','模拟面试-总结提示词','你负责输出最终面试反馈报告。请总结候选人的知识掌握情况、表达能力、风险点和下一步训练建议，要求专业、具体、可执行，能够直接作为训练反馈。','模拟面试总结提示词。',1,'2026-04-13 10:12:29','2026-04-13 10:12:29');
/*!40000 ALTER TABLE `ai_client_system_prompt` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `ai_client_tool_mcp` WRITE;
/*!40000 ALTER TABLE `ai_client_tool_mcp` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_client_tool_mcp` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
