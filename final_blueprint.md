# AI Agent Station 项目升级蓝图（修订版）

---

## 一、蓝图前提与重构约束

### 1.1 必须遵守的约束

1. 本次升级必须基于现有项目演进，不推倒重写。
2. 现有“根据数据库动态装配 Client/Model/MCP/Advisor 的代码链路”保留，不改动其核心设计。
3. MySQL 以本地 `127.0.0.1:13306 / ai-agent-station` 为准，已有可复用表优先复用，不重复造核心配置表。
4. 本地向量库以 Docker 中的 PostgreSQL 为准：
   - host: `127.0.0.1`
   - port: `5432`
   - username: `postgres`
   - password: `postgres`
   - database: `postgres`
   - 原有 PostgreSQL 表结构不作为本次设计约束，直接按新知识空间方案设计所需表与向量数据。
5. `ai_client_api` 保留现有可用 API 配置，尤其保留 DashScope 兼容端点：
   - `api_id=1003`
   - `base_url=https://dashscope.aliyuncs.com/compatible-mode`
6. `ai_client_model` 保留并复用 `2003-2006`，且统一修正为关联 `api_id=1003`：
   - `2003` 推理/主对话模型
   - `2004` 轻量对话模型
   - `2005` 向量模型
   - `2006` 重排模型
7. 旧的 `client / flow / prompt / advisor / tool_mcp` 配置不按原样继承，按新主线重新整理；能复用的保留，不能支撑新主线的清理或停用。

### 1.2 当前阶段目标

当前阶段不追求一次做全四个业务场景，而是先建立一条能演示、能面试讲、能继续扩展的主链路：

`Resume Knowledge Space -> 简历评估 -> 模拟面试`

这条链路完成后，再把同一套知识空间能力复用到：

- 文档知识助手
- 内容自动化

---

## 二、功能边界定义

### 2.1 最终产品边界

| Feature | 产品定义 | 输入 | 输出 | 是否第一阶段 |
|---------|----------|------|------|-------------|
| **简历评估** | 基于候选人简历知识空间的结构化评估能力 | PDF 简历 / 已有 Resume KS | 评分、优劣势、修改建议 | 是 |
| **模拟面试** | 基于同一 Resume KS 的多轮面试能力 | Resume KS + 多轮回答 | 追问、点评、面试总结 | 是 |
| **文档知识助手** | 面向上传文档空间的知识问答工作台，不做泛聊天 | 文档空间 + 问题 | 精准问答、摘要、测验 | 否，第二阶段 |
| **内容自动化** | 内容生产工作流，不是泛 Agent 聊天 | 主题/目标/渠道 | 生成、润色、审核、发布 | 否，第三阶段 |

### 2.2 第一阶段明确收敛

第一阶段只做一条纵深完整链路：

1. 上传 PDF 简历
2. 解析文本
3. 切片
4. 向量化入 PGVector
5. 建立 Resume Knowledge Space
6. 进行简历评估
7. 基于同一知识空间进入模拟面试
8. 输出面试反馈报告

### 2.3 不做的边界

1. 不做泛聊天机器人。
2. 不做重型 Multi-Agent 平台。
3. 不做复杂工作流编排中心后台。
4. 不在第一阶段做内容自动化发布闭环。
5. 不在第一阶段做完整成本平台或监控大盘。

---

## 三、现状诊断与保留策略

### 3.1 当前项目已具备的基础

#### A. 动态装配链路已成型，必须保留

当前项目已经具备：

- 从 MySQL 读取 `ai_client / ai_client_config / ai_client_api / ai_client_model / ai_client_advisor / ai_client_tool_mcp`
- 动态注册 `OpenAiApi / OpenAiChatModel / McpSyncClient / Advisor / ChatClient`
- 基于启动配置自动装配可执行 Client

这部分是项目最有价值的底座之一，保留不动。

#### B. Auto Agent 四步执行链路已成型，可抽象升级

现有链路为：

`分析 -> 执行 -> 质检 -> 总结`

这条链路已经能支撑：

- 多步执行
- SSE 流式反馈
- ChatMemory
- MCP 工具调用

但它目前更像“通用实验型 Auto Agent”，还不是面向产品场景的 Runtime。

#### C. RAG 基础能力已存在

项目已具备：

- PGVector
- `TokenTextSplitter`
- `RagAnswerAdvisor`
- `VectorStore.similaritySearch`

说明“知识空间产品化”不是从零开始，而是从已有 RAG 底座往上收敛。

#### D. PostgreSQL 向量库按新方案使用

本次向量库以本地 Docker PostgreSQL 为准：

- `127.0.0.1:5432`
- username: `postgres`
- password: `postgres`
- database: `postgres`

原有 PostgreSQL 表不作为迁移前提，也不要求兼容旧向量表结构。

### 3.2 当前缺口

1. 还没有“知识空间”业务模型。
2. 还没有“上传文档 -> 解析 -> 入库 -> 可追踪状态”的主流程。
3. Prompt 目前大量硬编码在执行节点中，尚未真正 DB 驱动。
4. 请求模型还停留在 `aiAgentId/message/sessionId/maxStep`，不支持 `agentType/subType/knowledgeSpaceId`。
5. 企业级能力没有形成完整审计闭环。

### 3.3 最值得保留的内容

1. 动态装配架构
2. 现有 Auto Agent 四步思想
3. SSE 流式交互
4. RAG 底座能力
5. MCP 接入方式

---

## 四、架构升级方向

### 4.1 四层架构

```
产品场景层
  ├── 简历评估
  ├── 模拟面试
  ├── 文档知识助手
  └── 内容自动化

Agent 编排层
  ├── Router
  ├── Planner
  ├── Executor
  ├── Verifier
  └── Summarizer

能力组件层
  ├── Resume Parsing
  ├── Chunking
  ├── Embedding
  ├── Retrieval
  ├── Rerank
  ├── Tool Calling
  ├── Prompt Registry
  └── Session Memory

基础设施层
  ├── MySQL
  ├── PostgreSQL / PGVector
  ├── Redis
  ├── Spring AI
  ├── MCP
  ├── SSE
  └── Logging / Trace / Audit
```

### 4.2 总体原则

1. `Armory` 负责装配，不负责业务运行时编排。
2. 新增 `Runtime` 负责业务场景执行。
3. ReAct 不作为总控架构，而作为某些步骤内的执行范式。
4. 固定 workflow 优先于重型 agentic 自主规划。

---

## 五、Agent Runtime 设计

### 5.1 统一 Runtime 形态

统一采用：

`Router -> Planner -> Executor -> Verifier -> Summarizer`

但不是所有场景都完整走五步。

### 5.2 不同场景的取舍

| 场景 | Runtime 策略 | 说明 |
|------|--------------|------|
| 简历评估 | Router -> Executor -> Verifier -> Summarizer | 以固定流程为主，不需要重规划 |
| 模拟面试 | Router -> Planner -> Executor -> Verifier -> Summarizer | Planner 主要决定下一轮提问策略 |
| 文档知识助手 | Router -> Executor -> Verifier | 问答/摘要/测验以固定 RAG 流程为主 |
| 内容自动化 | Router -> Planner -> Executor -> Verifier -> Summarizer | 最适合走工作流 |

### 5.3 第一阶段重点分支

第一阶段只重点建设两个运行分支：

#### 分支 A：简历评估

```
Router
  -> ResumeRetrieval
  -> ResumeAnalysis
  -> ScoreAndSuggestion
  -> Verifier
  -> Summarizer
```

#### 分支 B：模拟面试

```
Router
  -> InterviewPlanner
  -> InterviewQuestionExecutor
  -> CandidateAnswerEvaluator
  -> InterviewVerifier
  -> InterviewSummarizer
```

### 5.4 ReAct 的定位

ReAct 不作为系统顶层框架，而作为：

1. `Executor` 内部的子任务执行范式
2. 工具调用密集节点的思考模式
3. 内容自动化场景的执行细节策略

这样面试时可以讲得高级，但实现不会失控。

---

## 六、Skill / Tool / MCP / Feature 边界定义

### 6.1 概念定义

| 概念 | 定义 | 在本项目中的位置 |
|------|------|------------------|
| **Feature** | 用户可感知的业务功能 | 简历评估、模拟面试、文档知识助手、内容自动化 |
| **Skill** | 可复用领域能力包 | 简历分析能力、面试追问能力、文档摘要能力 |
| **Tool** | 最小原子动作 | PDF 解析、文本切片、向量检索、重排、CSDN 发帖 |
| **MCP** | 对外标准化能力接入层 | 搜索、发帖、外部系统调用 |

### 6.2 Skill 的落地定义

Skill 不定义成页面功能，也不定义成最小工具，而定义为：

`能力模板 + Prompt 策略 + 工具集绑定 + 输出校验规则`

例如：

#### ResumeAnalysisSkill

- Prompt：评估维度、岗位匹配、优劣势分析
- Tool：Resume Retrieval / Rerank
- Rule：必须输出评分、证据、修改建议

#### MockInterviewSkill

- Prompt：面试官设定、问题难度控制、追问策略
- Tool：Resume Retrieval / Session Memory
- Rule：必须输出问题、点评、阶段性结论

### 6.3 MCP 的定位

MCP 只承担标准接入，不承担场景定义。

也就是说：

- `CSDN MCP` 是能力接入层
- `内容自动化发布` 才是业务场景

---

## 七、数据库设计策略

### 7.1 核心原则

本次不重建现有核心配置表，只做：

1. 复用已有表
2. 修正必要数据
3. 新增业务表

### 7.2 必须复用的现有表

以下表直接复用，不新建替代版本：

```sql
ai_agent
ai_agent_flow_config
ai_agent_task_schedule
ai_client
ai_client_api
ai_client_model
ai_client_config
ai_client_system_prompt
ai_client_tool_mcp
ai_client_advisor
ai_client_rag_order
```

### 7.3 现有配置表的处理策略

#### A. `ai_client_api`

保留现有可用 API 配置，当前阶段主用：

```text
1003 -> https://dashscope.aliyuncs.com/compatible-mode
```

#### B. `ai_client_model`

保留并复用 `2003-2006`，并统一修正为关联 `1003`：

```text
2003 -> 主推理模型
2004 -> 轻量对话模型
2005 -> 向量模型
2006 -> 重排模型
```

说明：

1. 原库中 `2003-2006` 可保留。
2. 蓝图要求把它们统一整理到 DashScope 兼容 API 下。
3. 第一阶段的运行时 Client 只允许从这组模型中选型。

#### C. `ai_client / ai_client_config / ai_agent / ai_agent_flow_config`

不沿用旧业务配置，重新设计一套围绕“简历评估 + 模拟面试”的配置。

也就是说：

- 表结构复用
- 数据内容重建

#### D. `ai_client_system_prompt`

旧 Prompt 不默认继承。

策略：

1. 与第一阶段主线无关的 Prompt 清理或停用
2. 保留少量可复用通用 Prompt
3. 重新新增面试主线所需 Prompt

### 7.4 新增业务表

第一阶段建议新增以下表：

```sql
CREATE TABLE ai_knowledge_space (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    space_id      VARCHAR(64) NOT NULL UNIQUE,
    space_name    VARCHAR(128) NOT NULL,
    space_type    VARCHAR(32) NOT NULL COMMENT 'resume/document',
    biz_key       VARCHAR(64) DEFAULT NULL COMMENT '如 candidate_id',
    description   VARCHAR(512) DEFAULT NULL,
    status        TINYINT(1) DEFAULT 1,
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE ai_knowledge_document (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id        VARCHAR(64) NOT NULL UNIQUE,
    space_id      VARCHAR(64) NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    file_type     VARCHAR(32) NOT NULL,
    file_size     BIGINT DEFAULT 0,
    file_path     VARCHAR(512) DEFAULT NULL,
    parse_status  VARCHAR(32) DEFAULT 'pending',
    chunk_count   INT DEFAULT 0,
    vector_status VARCHAR(32) DEFAULT 'pending',
    status        TINYINT(1) DEFAULT 1,
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE ai_knowledge_chunk (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    chunk_id      VARCHAR(64) NOT NULL UNIQUE,
    doc_id        VARCHAR(64) NOT NULL,
    space_id      VARCHAR(64) NOT NULL,
    chunk_index   INT NOT NULL,
    chunk_text    TEXT NOT NULL,
    metadata_json JSON DEFAULT NULL,
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_interview_session (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    interview_id    VARCHAR(64) NOT NULL UNIQUE,
    space_id        VARCHAR(64) NOT NULL,
    session_id      VARCHAR(64) NOT NULL,
    interview_state VARCHAR(32) DEFAULT 'running',
    round_count     INT DEFAULT 0,
    summary_json    JSON DEFAULT NULL,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE ai_execution_log (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    trace_id          VARCHAR(64) NOT NULL,
    session_id        VARCHAR(64) NOT NULL,
    agent_id          VARCHAR(64) DEFAULT NULL,
    agent_type        VARCHAR(32) NOT NULL,
    sub_type          VARCHAR(32) DEFAULT NULL,
    node_type         VARCHAR(64) NOT NULL,
    input_summary     TEXT DEFAULT NULL,
    output_summary    TEXT DEFAULT NULL,
    prompt_tokens     INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    total_tokens      INT DEFAULT 0,
    duration_ms       BIGINT DEFAULT 0,
    status            VARCHAR(32) DEFAULT 'success',
    error_message     TEXT DEFAULT NULL,
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

说明：

1. 上述业务表建在本地 PostgreSQL `postgres` 数据库中。
2. 原来的 PostgreSQL 表不用关心，不纳入本次蓝图。
3. PGVector 向量表可直接按新命名规范创建，避免与旧表耦合。

### 7.5 不建议新增的表

当前阶段不建议新建：

```text
ai_agent_branch_config
ai_runtime_node_config
ai_skill_definition
```

原因：

它们会和现有表职责重叠，先把现有表用顺比再造新抽象更重要。

---

## 八、第一阶段配置重建方案

### 8.1 第一阶段建议保留/重建的 Agent

| agent_id | agent_name | 用途 |
|----------|------------|------|
| A1001 | Resume Evaluation Agent | 简历评估 |
| A1002 | Mock Interview Agent | 模拟面试 |

### 8.2 第一阶段建议重建的 Client

建议围绕两个 Agent 重建 6 类 Client：

| client_id | client_name | 模型建议 |
|-----------|-------------|---------|
| C1101 | Resume Retrieval Client | 2005 / 2006 参与检索与重排 |
| C1102 | Resume Analysis Client | 2003 |
| C1103 | Resume Verify Client | 2003 或 2004 |
| C1201 | Interview Planner Client | 2003 |
| C1202 | Interview Executor Client | 2003 |
| C1203 | Interview Summary Client | 2003 或 2004 |

### 8.3 第一阶段 Prompt 规划

第一阶段建议最少只保留/新增以下 Prompt：

1. 简历评估 Prompt
2. 简历评分 Prompt
3. 简历建议 Prompt
4. 面试官 Prompt
5. 候选人回答评估 Prompt
6. 面试总结 Prompt
7. 通用校验 Prompt

### 8.4 第一阶段 MCP 策略

第一阶段面试主线不依赖外部 MCP 才能成立。

因此：

1. 面试主线默认不强绑定 CSDN / 搜索类 MCP
2. MCP 留作第二阶段扩展能力
3. 第一阶段先把“无外部依赖也能跑通”的闭环做完整

---

## 九、前后端主流程设计

### 9.1 Resume Knowledge Space 建立流程

```text
前端上传 PDF
-> 后端创建 Resume Knowledge Space
-> 保存 document 记录
-> 解析 PDF
-> 切片
-> 写入 ai_knowledge_chunk
-> 向量化写入 PGVector
-> 更新 document/vector 状态
```

### 9.2 简历评估流程

```text
用户选择简历
-> 请求 Resume Evaluation Agent
-> 根据 Resume KS 检索上下文
-> LLM 输出评分、优劣势、建议
-> Verifier 检查结构完整性
-> SSE 返回过程和最终结果
```

### 9.3 模拟面试流程

```text
用户从简历评估进入模拟面试
-> 创建 interview_session
-> Planner 根据 Resume KS 生成问题
-> 用户回答
-> Evaluator 评估回答并决定是否追问
-> 多轮循环
-> 最终输出面试反馈报告
```

### 9.4 第一阶段 API 建议

```text
POST   /api/v1/resume/upload
POST   /api/v1/resume/evaluate
POST   /api/v1/interview/start
POST   /api/v1/interview/answer
GET    /api/v1/interview/{id}
GET    /api/v1/knowledge/space/{spaceId}
```

统一请求体新增字段：

```json
{
  "agentType": "resume | interview",
  "subType": "resume_eval | mock_interview",
  "knowledgeSpaceId": "ks_resume_001",
  "sessionId": "session_001",
  "message": "请开始模拟面试",
  "maxStep": 5
}
```

---

## 十、企业级能力优先级

### 10.1 第一阶段就要做的

| 能力 | 原因 |
|------|------|
| traceId 全链路追踪 | 面试价值高，实现成本低 |
| SSE 过程事件流 | 能直观看到 Agent 过程 |
| 节点级执行日志 | 方便定位问题，也能讲工程化 |
| 结构化输出校验 | 降低 LLM 不稳定性 |
| 检索记录 | 面试时能讲 RAG 可追溯 |

### 10.2 第二阶段做

| 能力 | 原因 |
|------|------|
| rerank | 文档助手阶段价值更高 |
| 多路召回 | 文档助手阶段再扩展 |
| token 用量记录 | 需要拦截器和成本归集 |
| 工具调用记录 | 内容自动化更需要 |

### 10.3 亮点但不急着做

| 能力 | 原因 |
|------|------|
| 成本预算控制 | 工程价值高，但第一阶段收益不够大 |
| metrics/dashboard | 演示价值不如主链路 |
| 自动路由 | 容易把系统复杂化 |

---

## 十一、分阶段开发计划

### Phase 0：配置收敛与 Runtime 基础改造

**目标**：不动动态装配代码，先把配置体系和运行时入口收敛正确。

任务：

1. 核对并保留 `ai_client_api=1003`
2. 保留并修正 `ai_client_model=2003-2006 -> api_id=1003`
3. 清理旧的无关 `client/config/prompt/flow`
4. 重新配置面试主线所需 Agent / Client / Prompt / Flow
5. 扩展统一请求模型，支持 `agentType/subType/knowledgeSpaceId`
6. 给 Runtime 增加 `traceId`

验收标准：

1. 动态装配仍能正常工作
2. 新配置能装配出面试主线 Client
3. API/Model 未被破坏
4. 不新增替代性核心配置表

### Phase 1：Resume Knowledge Space + 简历评估

**目标**：跑通从 PDF 到评估报告的完整链路。

任务：

1. 新增 `ai_knowledge_space / ai_knowledge_document / ai_knowledge_chunk`
2. 实现上传 PDF
3. 实现解析、切片、向量化
4. 建立 Resume KS
5. 实现 Resume Evaluation Runtime
6. 输出结构化评估结果

验收标准：

1. 能上传 PDF 简历
2. 能生成 Resume KS
3. 能返回评分、优劣势、修改建议
4. 有完整 SSE 事件流和执行日志

### Phase 2：模拟面试

**目标**：基于同一 Resume KS 跑通多轮面试。

任务：

1. 新增 `ai_interview_session`
2. 实现面试启动
3. 实现问题生成
4. 实现回答评估与追问
5. 实现面试总结报告

验收标准：

1. 简历评估后可直接进入模拟面试
2. 多轮会话可持续
3. 最终产出面试反馈报告
4. 与 Resume KS 共用同一知识上下文

### Phase 3：文档知识助手

**目标**：把 Resume KS 的知识能力抽象复用到文档空间。

任务：

1. 扩展 `space_type=document`
2. 实现文档问答
3. 实现摘要
4. 实现测验化输出
5. 引入 rerank

### Phase 4：内容自动化

**目标**：做“生成 -> 润色 -> 审核 -> 发布”的工作流。

任务：

1. 增加 Planner/ToolExecution/Verifier 工作流
2. 将发帖动作抽为 MCP Tool
3. 接入 CSDN 发布

### Phase 5：企业级工程增强

任务：

1. token/cost 记录
2. 工具调用记录
3. 检索记录完善
4. metrics / trace / 监控增强

---

## 十二、最适合面试表达的亮点

1. **保留原有数据驱动装配底座，在此基础上做产品化升级**  
   不是推翻重写，而是在原有动态装配架构上增加 Runtime 和知识空间能力。

2. **把通用 Auto Agent 收敛成可落地的产品场景 Runtime**  
   从实验型多步 Agent，演进为面向简历评估和模拟面试的固定编排引擎。

3. **知识空间复用设计**  
   简历评估和模拟面试共用同一 Resume Knowledge Space，体现可复用和业务抽象能力。

4. **配置驱动 + 模型分工**  
   推理模型、向量模型、重排模型通过数据库配置编排，不需要改代码切模型。

5. **企业级可追溯**  
   SSE 过程流、traceId、节点执行日志、检索记录，能够解释 Agent 每一步如何得出结果。

6. **技术取舍清晰**  
   不上重型 Multi-Agent，不做泛聊天，优先做固定 workflow + 局部 agentic execution。

---

## 十三、本版蓝图的最终结论

1. 第一阶段主线确定为：
   `Resume Knowledge Space -> 简历评估 -> 模拟面试`

2. 核心配置表策略确定为：
   `复用现表，不新建替代性核心配置表`

3. 配置数据策略确定为：
   `保留 ai_client_api 和 ai_client_model（主用 1003 + 2003-2006），重做 client/flow/prompt 等业务配置`

4. Runtime 策略确定为：
   `统一 Router -> Planner -> Executor -> Verifier -> Summarizer，按场景裁剪`

5. Skill 定义确定为：
   `可复用领域能力包，而不是页面功能，也不是最小工具`

6. MCP 定义确定为：
   `标准化能力接入层，不直接承担业务场景定义`
