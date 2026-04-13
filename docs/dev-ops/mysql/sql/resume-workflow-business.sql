SET NAMES utf8mb4;
USE `ai-agent-station`;

CREATE TABLE IF NOT EXISTS `resume_profile` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `file_name` VARCHAR(255) NOT NULL,
    `file_hash` VARCHAR(64) DEFAULT NULL,
    `raw_text` LONGTEXT,
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `resume_knowledge_space` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `resume_id` BIGINT NOT NULL,
    `knowledge_tag` VARCHAR(64) NOT NULL,
    `vector_table` VARCHAR(64) NOT NULL,
    `chunk_count` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `resume_interview_session` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `resume_id` BIGINT NOT NULL,
    `knowledge_space_id` BIGINT NOT NULL,
    `session_code` VARCHAR(64) NOT NULL,
    `opening_questions` TEXT,
    `current_round` INT DEFAULT 1,
    `status` VARCHAR(32) DEFAULT 'INIT',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `resume_interview_round` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `interview_session_id` BIGINT NOT NULL,
    `round_no` INT NOT NULL,
    `question_content` TEXT,
    `answer_content` TEXT,
    `feedback_content` TEXT,
    `next_question` TEXT,
    `status` VARCHAR(32) DEFAULT 'INIT',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
