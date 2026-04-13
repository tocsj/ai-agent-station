SET NAMES utf8mb4;
USE `ai-agent-station`;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `ai_agent_task_schedule`;
DELETE FROM `ai_agent_flow_config`;
DELETE FROM `ai_client_config`;
DELETE FROM `ai_client_rag_order`;
DELETE FROM `ai_client_tool_mcp`;
DELETE FROM `ai_client_advisor`;
DELETE FROM `ai_client_system_prompt`;
DELETE FROM `ai_client`;
DELETE FROM `ai_agent`;

SET FOREIGN_KEY_CHECKS = 1;

UPDATE `ai_client_api`
SET
    `base_url` = 'https://dashscope.aliyuncs.com/compatible-mode/',
    `status` = 1,
    `update_time` = NOW()
WHERE `api_id` = '1003';

UPDATE `ai_client_model`
SET
    `api_id` = '1003',
    `status` = 1,
    `update_time` = NOW()
WHERE `model_id` IN ('2003', '2004', '2005', '2006');

INSERT INTO `ai_agent` (
    `agent_id`,
    `agent_name`,
    `description`,
    `channel`,
    `status`,
    `create_time`,
    `update_time`
) VALUES
(
    '1001',
    '简历评估智能体',
    '基于候选人简历知识空间执行简历评估。',
    'agent',
    1,
    NOW(),
    NOW()
),
(
    '1002',
    '模拟面试智能体',
    '基于候选人简历知识空间执行模拟面试。',
    'agent',
    1,
    NOW(),
    NOW()
);

INSERT INTO `ai_client` (
    `client_id`,
    `client_name`,
    `description`,
    `status`,
    `create_time`,
    `update_time`
) VALUES
('5101', '简历评估-任务分析', '简历评估任务拆解客户端', 1, NOW(), NOW()),
('5102', '简历评估-执行评估', '简历评估执行客户端', 1, NOW(), NOW()),
('5103', '简历评估-质量复核', '简历评估质量复核客户端', 1, NOW(), NOW()),
('5104', '简历评估-结果总结', '简历评估总结客户端', 1, NOW(), NOW()),
('5201', '模拟面试-任务分析', '模拟面试任务拆解客户端', 1, NOW(), NOW()),
('5202', '模拟面试-面试执行', '模拟面试执行客户端', 1, NOW(), NOW()),
('5203', '模拟面试-质量复核', '模拟面试质量复核客户端', 1, NOW(), NOW()),
('5204', '模拟面试-结果总结', '模拟面试总结客户端', 1, NOW(), NOW());

INSERT INTO `ai_client_advisor` (
    `advisor_id`,
    `advisor_name`,
    `advisor_type`,
    `order_num`,
    `ext_param`,
    `status`,
    `create_time`,
    `update_time`
) VALUES
(
    '4001',
    'chat-memory',
    'ChatMemory',
    1,
    '{"maxMessages":200}',
    1,
    NOW(),
    NOW()
),
(
    '4002',
    'resume-knowledge-rag',
    'RagAnswer',
    2,
    '{"topK":6,"filterExpression":"knowledge == ''resume_knowledge_space''"}',
    1,
    NOW(),
    NOW()
);

INSERT INTO `ai_client_rag_order` (
    `rag_id`,
    `rag_name`,
    `knowledge_tag`,
    `status`,
    `create_time`,
    `update_time`
) VALUES
(
    '9001',
    'Resume Knowledge Space',
    'resume_knowledge_space',
    1,
    NOW(),
    NOW()
);

INSERT INTO `ai_client_system_prompt` (
    `prompt_id`,
    `prompt_name`,
    `prompt_content`,
    `description`,
    `status`,
    `create_time`,
    `update_time`
) VALUES
(
    '9101',
    '简历评估-任务分析提示词',
    '你负责把候选人的问题和简历内容拆解为评估任务。请输出：评估目标、关键信息缺口、需要重点核验的经历、后续执行步骤。要求结构清晰，避免空泛表述。',
    '简历评估任务分析提示词。',
    1,
    NOW(),
    NOW()
),
(
    '9102',
    '简历评估-执行提示词',
    '你负责执行简历评估。请结合候选人简历知识空间，输出岗位匹配度、技术深度、项目真实性信号、优势、短板和修改建议。结论必须具体，能够直接用于候选人改简历。',
    '简历评估执行提示词。',
    1,
    NOW(),
    NOW()
),
(
    '9103',
    '简历评估-复核提示词',
    '你负责质量复核。请检查评估结果是否存在证据不足、结论跳跃、缺少引用、建议不可执行等问题，并给出修正意见。你的职责不是重复总结，而是指出漏洞。',
    '简历评估复核提示词。',
    1,
    NOW(),
    NOW()
),
(
    '9104',
    '简历评估-总结提示词',
    '你负责输出最终简历评估报告。请按总体结论、优势亮点、主要风险、优化建议四个部分组织内容，要求专业、直接、可执行，适合直接给候选人查看。',
    '简历评估总结提示词。',
    1,
    NOW(),
    NOW()
),
(
    '9201',
    '模拟面试-任务分析提示词',
    '你负责生成模拟面试计划。请基于候选人简历知识空间，明确本轮面试目标、题目范围、难度分层、重点追问点和预期考察能力。',
    '模拟面试任务分析提示词。',
    1,
    NOW(),
    NOW()
),
(
    '9202',
    '模拟面试-执行提示词',
    '你负责推进模拟面试。请逐轮提出问题，给出本轮评价维度和追问策略，并根据候选人的回答动态调整难度和追问方向，让面试过程更像真实技术面试。',
    '模拟面试执行提示词。',
    1,
    NOW(),
    NOW()
),
(
    '9203',
    '模拟面试-复核提示词',
    '你负责复核模拟面试过程。请检查题目是否贴合简历、难度是否合理、追问是否有效、评价是否有依据，并指出需要修正的地方，避免形式化面试。',
    '模拟面试复核提示词。',
    1,
    NOW(),
    NOW()
),
(
    '9204',
    '模拟面试-总结提示词',
    '你负责输出最终面试反馈报告。请总结候选人的知识掌握情况、表达能力、风险点和下一步训练建议，要求专业、具体、可执行，能够直接作为训练反馈。',
    '模拟面试总结提示词。',
    1,
    NOW(),
    NOW()
);

INSERT INTO `ai_agent_flow_config` (
    `agent_id`,
    `client_id`,
    `client_name`,
    `client_type`,
    `sequence`,
    `step_prompt`,
    `create_time`
) VALUES
('1001', '5101', '简历评估-任务分析', 'TASK_ANALYZER_CLIENT', 1, '拆解简历评估任务', NOW()),
('1001', '5102', '简历评估-执行评估', 'PRECISION_EXECUTOR_CLIENT', 2, '执行简历评估', NOW()),
('1001', '5103', '简历评估-质量复核', 'QUALITY_SUPERVISOR_CLIENT', 3, '复核评估质量', NOW()),
('1001', '5104', '简历评估-结果总结', 'RESPONSE_ASSISTANT', 4, '生成最终简历评估报告', NOW()),
('1002', '5201', '模拟面试-任务分析', 'TASK_ANALYZER_CLIENT', 1, '拆解模拟面试计划', NOW()),
('1002', '5202', '模拟面试-面试执行', 'PRECISION_EXECUTOR_CLIENT', 2, '推进模拟面试轮次', NOW()),
('1002', '5203', '模拟面试-质量复核', 'QUALITY_SUPERVISOR_CLIENT', 3, '复核面试质量', NOW()),
('1002', '5204', '模拟面试-结果总结', 'RESPONSE_ASSISTANT', 4, '输出最终面试反馈报告', NOW());

INSERT INTO `ai_client_config` (
    `source_type`,
    `source_id`,
    `target_type`,
    `target_id`,
    `ext_param`,
    `status`,
    `create_time`,
    `update_time`
) VALUES
('client', '5101', 'model', '2003', '\"\"', 1, NOW(), NOW()),
('client', '5101', 'prompt', '9101', '\"\"', 1, NOW(), NOW()),
('client', '5101', 'advisor', '4001', '\"\"', 1, NOW(), NOW()),
('client', '5101', 'advisor', '4002', '\"\"', 1, NOW(), NOW()),
('client', '5102', 'model', '2003', '\"\"', 1, NOW(), NOW()),
('client', '5102', 'prompt', '9102', '\"\"', 1, NOW(), NOW()),
('client', '5102', 'advisor', '4001', '\"\"', 1, NOW(), NOW()),
('client', '5102', 'advisor', '4002', '\"\"', 1, NOW(), NOW()),
('client', '5103', 'model', '2004', '\"\"', 1, NOW(), NOW()),
('client', '5103', 'prompt', '9103', '\"\"', 1, NOW(), NOW()),
('client', '5103', 'advisor', '4001', '\"\"', 1, NOW(), NOW()),
('client', '5103', 'advisor', '4002', '\"\"', 1, NOW(), NOW()),
('client', '5104', 'model', '2003', '\"\"', 1, NOW(), NOW()),
('client', '5104', 'prompt', '9104', '\"\"', 1, NOW(), NOW()),
('client', '5104', 'advisor', '4001', '\"\"', 1, NOW(), NOW()),
('client', '5104', 'advisor', '4002', '\"\"', 1, NOW(), NOW()),
('client', '5201', 'model', '2003', '\"\"', 1, NOW(), NOW()),
('client', '5201', 'prompt', '9201', '\"\"', 1, NOW(), NOW()),
('client', '5201', 'advisor', '4001', '\"\"', 1, NOW(), NOW()),
('client', '5201', 'advisor', '4002', '\"\"', 1, NOW(), NOW()),
('client', '5202', 'model', '2003', '\"\"', 1, NOW(), NOW()),
('client', '5202', 'prompt', '9202', '\"\"', 1, NOW(), NOW()),
('client', '5202', 'advisor', '4001', '\"\"', 1, NOW(), NOW()),
('client', '5202', 'advisor', '4002', '\"\"', 1, NOW(), NOW()),
('client', '5203', 'model', '2004', '\"\"', 1, NOW(), NOW()),
('client', '5203', 'prompt', '9203', '\"\"', 1, NOW(), NOW()),
('client', '5203', 'advisor', '4001', '\"\"', 1, NOW(), NOW()),
('client', '5203', 'advisor', '4002', '\"\"', 1, NOW(), NOW()),
('client', '5204', 'model', '2003', '\"\"', 1, NOW(), NOW()),
('client', '5204', 'prompt', '9204', '\"\"', 1, NOW(), NOW()),
('client', '5204', 'advisor', '4001', '\"\"', 1, NOW(), NOW()),
('client', '5204', 'advisor', '4002', '\"\"', 1, NOW(), NOW());
