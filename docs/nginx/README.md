# Auto-Agent Chat Frontend (temp02)

## 1. 后端接口契约（来自代码）

来源文件：
- `ai-agent-station-trigger/src/main/java/com/tkck/trigger/http/AiAgentController.java`
- `ai-agent-station-api/src/main/java/com/tkck/api/dto/AutoAgentRequestDTO.java`
- `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/model/entity/AutoAgentExecuteResultEntity.java`
- `ai-agent-station-domain/src/main/java/com/tkck/domain/agent/service/execute/auto/step/*.java`

**Endpoint**
- `POST /api/v1/agent/auto_agent`

**Request JSON**
```json
{
  "aiAgentId": "<string>",
  "message": "<string>",
  "sessionId": "<string>",
  "maxStep": 3
}
```

**Response (SSE, text/event-stream)**
- `data: {json}\n\n` 的 SSE 事件流
- `content-type: text/event-stream`
- 事件 JSON 字段：
```json
{
  "type": "analysis|execution|supervision|summary|error|complete",
  "subType": "analysis_status|analysis_history|analysis_strategy|analysis_progress|analysis_task_status|execution_target|execution_process|execution_result|execution_quality|assessment|issues|suggestions|score|pass|completed_work|incomplete_reasons|key_factors|efficiency_quality|suggestions|evaluation|summary_overview",
  "step": 1,
  "content": "<string>",
  "completed": false,
  "timestamp": 1738040000000,
  "sessionId": "<string>"
}
```

**流式完成标记**
- `type: "complete"` 表示结束
- summary 阶段会发送 `type: "summary"`（整体结果）以及 `summary` 的 subType 细分

**异常情况**
- controller 里的异常可能直接 `emitter.send("执行异常...")`，不一定是 SSE JSON，前端已做兜底处理。

**CORS**
- `@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {GET, POST, OPTIONS})`

## 2. 前端数据模型映射

- Conversation.sessionId = `sessionId`（直接使用前端会话 id）
- Message.role = user/assistant/system
- StepEvent 映射：
  - StepEvent.type = `type`
  - StepEvent.subType = `subType`
  - StepEvent.step = `step`
  - StepEvent.content = `content`
  - StepEvent.timestamp = `timestamp`
  - StepEvent.sessionId = `sessionId`
- summary 的 content 直接映射为聊天窗口中的 assistant 回复。
- analysis/execution/supervision/summary 的 subType 全部进入「过程面板」。

## 3. 本地开发

前端源码目录：`docs/nginx/temp02/app`

```powershell
# 可选：复制示例环境变量
Copy-Item .env.example .env

# 启动 dev server
.\docs\nginx\temp02\scripts\dev.ps1
```

默认端口：`http://localhost:5173`

## 4. 构建

```powershell
.\docs\nginx\temp02\scripts\build.ps1
```

构建产物默认输出到：`docs/nginx/temp02/dist`

## 5. Nginx 部署

### 5.1 直接使用 nginx.conf
- 配置文件：`docs/nginx/temp02/nginx.conf`
- 默认监听：`8088`
- 静态目录：`docs/nginx/temp02/dist`
- 代理后端：`http://127.0.0.1:8091`

```bash
nginx -c D:/JAVA/study-project/ai-agent-station-new/docs/nginx/temp02/nginx.conf
```

### 5.2 Docker Compose
```bash
docker compose -f docs/nginx/temp02/docker-compose.yml up -d
```

## 6. 后端地址与鉴权

- 本项目后端未看到鉴权逻辑（接口未要求 token）。
- 前端默认使用相对地址 `/api`，你也可在 `.env` 中设置：
```
VITE_API_BASE=/api
```

## 7. 常见问题

- **SSE 被 nginx 缓冲**：必须设置 `proxy_buffering off;`，已在 nginx.conf 中配置。
- **CORS**：后端 controller 已允许 `*`，如需严格化请在后端自定义。
- **SSE 断连**：前端做了 1 次自动重连，并在过程面板里记录。
