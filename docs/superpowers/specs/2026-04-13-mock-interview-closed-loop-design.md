# 模拟面试固定三轮闭环设计

## 目标

在现有 `Resume Knowledge Space -> 简历评估 -> 模拟面试` 主链路上，将模拟面试从“可启动、可答题”补齐为“固定三轮、每轮有点评、最终有总结、全程可追踪”的完整闭环。

本次设计坚持以下约束：

1. 保留现有数据库动态装配 `Client / Model / Advisor / MCP` 主链路，不推倒重写。
2. 复杂场景继续复用原有四步循环 Agent 链路：`分析 -> 执行 -> 复核 -> 总结`。
3. 简单场景继续采用单次 LLM 调用，不人为复杂化。
4. 第一版面试闭环采用固定三轮，不引入动态轮次决策。

## 范围

### 本次要完成

1. 创建面试会话
2. 生成第 1 轮开场问题
3. 固定完成 3 轮问答
4. 每轮回答后返回：
   - 本轮点评
   - 本轮分数
   - 本轮薄弱点
   - 下一轮问题（第 1、2 轮）
   - 或最终总结报告（第 3 轮）
5. 持久化面试轮次状态
6. SSE 返回每轮 Agent 过程事件

### 本次不做

1. 动态轮次数量控制
2. 面试过程中动态切换面试风格
3. 多岗位多模板策略系统
4. 企业级监控大盘
5. 真正的 Tool/MCP 驱动面试流程

## 现状诊断

当前模拟面试已有两段能力：

1. `POST /api/v1/resume/interview/start`
   - 单次读取 `resume_profile.raw_text`
   - 生成开场问题
   - 创建 `resume_interview_session`

2. `POST /api/v1/resume/interview/answer/stream`
   - 构建 `aiAgentId=1002`
   - 进入四步循环 Agent Runtime

当前缺口在于：

1. 面试轮次数据没有形成完整状态机
2. 每轮回答后的结果没有稳定结构化
3. 第 1、2 轮没有稳定产出“下一题”
4. 第 3 轮没有稳定产出“最终面试报告”
5. 前端难以根据后端返回展示完整面试工作台

## 方案选择

### 方案 A：轻入口 + 重评估

- `interview/start` 保持轻量，负责创建会话和首轮问题
- 每次回答调用一次四步链路，完成：
  - 本轮点评
  - 下一轮题目
  - 或最终总结

这是本次采用方案。

### 为什么不选全链路统一 Agent

如果把三轮全部塞进单次长链路 Runtime：

1. 前端交互会变复杂
2. 用户回答插入点不好控制
3. 会话恢复与失败重试成本高
4. 不利于当前阶段快速闭环

## 运行形态

### 轻链路

1. 上传简历
2. 开始面试
3. 查询面试状态

### 重链路

1. 简历评估
2. 面试答题评估

这保持了当前项目的统一策略：

- 复杂场景走四步循环 Agent
- 简单动作单次 LLM

## 固定三轮状态机

### 会话状态

`CREATED -> STARTED -> IN_PROGRESS -> FINISHED`

### 轮次状态

`ASKED -> ANSWERED -> EVALUATED`

### 三轮流程

#### 第 1 轮

1. `interview/start` 生成首轮问题
2. 保存 round 1 问题记录，状态 `ASKED`
3. 用户提交回答
4. `interview/answer/stream` 评估 round 1
5. 返回 round 1 点评和 round 2 问题
6. 落库 round 1 结果，并创建 round 2 问题记录

#### 第 2 轮

1. 用户提交 round 2 回答
2. 评估 round 2
3. 返回 round 2 点评和 round 3 问题
4. 落库 round 2 结果，并创建 round 3 问题记录

#### 第 3 轮

1. 用户提交 round 3 回答
2. 评估 round 3
3. 返回 round 3 点评和最终面试总结
4. 落库 round 3 结果，更新 session 为 `FINISHED`

## 数据设计

### `resume_interview_session`

建议补齐以下语义：

- `id`
- `resume_id`
- `knowledge_space_id`
- `session_code`
- `opening_questions`
- `current_round`
- `total_rounds`，固定为 3
- `status`
- `final_report`
- `create_time`
- `update_time`

说明：
现有表如果没有 `total_rounds` 和 `final_report`，本次建议增量补齐，不另建替代表。

### `resume_interview_round`

建议承载：

- `interview_session_id`
- `round_no`
- `question_content`
- `answer_content`
- `evaluation_content`
- `score`
- `status`
- `create_time`
- `update_time`

说明：
本轮“下一题”不直接存在上一轮记录里，而是在创建下一轮记录时作为 `question_content` 保存。

## 每轮输出结构

虽然底层仍通过 SSE 返回 `type/subType/content` 文本事件，但业务上需要统一成以下语义：

### 第 1、2 轮

1. 本轮评分
2. 本轮优点
3. 本轮问题点
4. 本轮薄弱项
5. 下一轮问题
6. 当前是否结束：`false`

### 第 3 轮

1. 本轮评分
2. 本轮优点
3. 本轮问题点
4. 本轮薄弱项
5. 最终面试总结报告
6. 当前是否结束：`true`

## 四步链路职责

### Step1 Analyzer

负责：

1. 理解当前轮的目标
2. 结合 Resume KS 分析候选人回答
3. 确定本轮关注点

约束：

1. 不允许虚构工具调用
2. 知识检索依赖内置 RAG Advisor

### Step2 Executor

负责：

1. 生成本轮评价
2. 识别薄弱点
3. 生成下一轮题目或第 3 轮总结草稿

### Step3 Verifier

负责：

1. 检查评价是否具体
2. 检查是否贴合简历内容
3. 检查是否真正给出追问或总结

### Step4 Summarizer

负责：

1. 第 1、2 轮输出“本轮点评 + 下一轮问题”
2. 第 3 轮输出“本轮点评 + 最终面试报告”

## 接口设计收敛

### 保持已有接口不变

1. `POST /api/v1/resume/interview/start`
2. `POST /api/v1/resume/interview/answer/stream`

### 建议新增接口

#### 查询面试会话详情

`GET /api/v1/resume/interview/{interviewSessionId}`

用途：

1. 前端刷新恢复
2. 展示轮次进度
3. 加载历史问题与回答
4. 展示最终报告

建议返回：

- session 基本信息
- 当前轮次
- 总轮次
- status
- rounds 列表
- finalReport

## 前端配合方式

### 面试工作台布局

#### 左侧

1. 面试配置
2. 当前轮次 `1/3`
3. 状态标签

#### 中间

1. 当前问题
2. 回答输入框
3. 历史轮次记录

#### 右侧

1. 当前轮评分
2. 当前轮优点
3. 当前轮问题点
4. 薄弱项
5. 命中简历片段
6. Agent 过程时间线

### 流式事件显示

前端仍按当前真实 SSE 事件模型显示：

- `analysis`
- `execution`
- `supervision`
- `summary`
- `complete`
- `error`

但业务呈现上应归并为：

1. 本轮过程
2. 本轮结果
3. 下一轮动作

## 验收标准

1. 能从简历评估页进入模拟面试
2. 能创建固定三轮的面试会话
3. 能完成三轮问答
4. 每轮都能返回点评和分数
5. 第 1、2 轮返回下一题
6. 第 3 轮返回最终面试总结
7. 能通过会话详情接口恢复历史轮次
8. SSE 过程在前端可展示

## 面试表达建议

这条链路可以这样表述：

1. 候选人知识空间负责提供检索增强上下文
2. 面试主流程采用固定三轮状态机，保证产品体验稳定
3. 每轮内部通过四步 Agent Runtime 完成分析、评估、复核和总结
4. 复杂逻辑保留 Agent 能力，简单入口保持轻量
5. 整个流程具备可追踪性、可复盘性和后续扩展能力
