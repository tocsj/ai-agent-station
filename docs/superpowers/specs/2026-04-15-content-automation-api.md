# Content Automation Frontend API

Base path: `/api/v1/content`

Current scope:
- content automation v1
- structured execution flow
- mock draft publish only
- history dialog support
- juejin global channel config and token verify

## 1. Create Task

`POST /task/create`

Request:

```json
{
  "topic": "企业级 AI Agent 编排平台",
  "platform": "公众号",
  "style": "专业",
  "keywords": "AI Agent,Java,DDD",
  "channel": "mock"
}
```

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "taskId": 21,
    "taskCode": "ct_021",
    "executionMode": "STRUCTURED_PLAN_EXECUTE",
    "status": "CREATED",
    "currentStep": "CREATED"
  }
}
```

Frontend usage:
- create task first
- store `taskId`
- use `taskId` for stream execution and detail queries

## 2. Execute Task Stream

`POST /task/execute/stream`

Headers:
- `Content-Type: application/json`
- response `text/event-stream`

Request:

```json
{
  "taskId": 21,
  "sessionId": "content-21",
  "maxStep": 8
}
```

SSE event payload:

```json
{
  "type": "content_step",
  "taskId": 21,
  "stepNo": 1,
  "stepName": "topic_plan",
  "status": "COMPLETED",
  "content": "输出内容",
  "completed": false,
  "timestamp": 1776224300000
}
```

Complete event:

```json
{
  "type": "content_complete",
  "taskId": 21,
  "status": "COMPLETED",
  "content": "最终总结",
  "completed": true,
  "timestamp": 1776224309999
}
```

Error event:

```json
{
  "type": "content_error",
  "taskId": 21,
  "status": "ERROR",
  "content": "错误信息",
  "completed": true,
  "timestamp": 1776224309999
}
```

`stepName` enum:
- `topic_plan`
- `outline`
- `draft`
- `polish`
- `compliance`
- `publish_plan`
- `publish_execute`
- `publish_summary`

`status` enum:
- `COMPLETED`
- `DEGRADED`
- `ERROR`

Frontend usage:
- append timeline by `stepNo`
- update main center panel by `stepName`
- when `type=content_complete`, stop loading and refresh detail/steps

## 3. Query Task Detail

`GET /task/{taskId}`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "taskId": 21,
    "taskCode": "ct_021",
    "executionMode": "STRUCTURED_PLAN_EXECUTE",
    "topic": "企业级 AI Agent 编排平台",
    "platform": "公众号",
    "style": "专业",
    "keywords": "AI Agent,Java,DDD",
    "channel": "mock",
    "status": "COMPLETED",
    "currentStep": "COMPLETED",
    "title": "标题",
    "outlineText": "大纲",
    "draftContent": "初稿",
    "finalContent": "润色后正文",
    "complianceResult": "审核结果",
    "publishStatus": "DRAFT_SAVED",
    "publishExternalId": "draft_21",
    "publishExternalUrl": "https://mock-publish.local/draft/draft_21",
    "summaryText": "执行总结",
    "createTime": "2026-04-15 11:40:00.0",
    "updateTime": "2026-04-15 11:41:00.0"
  }
}
```

Frontend usage:
- recover page state after refresh
- fill center editor/result area
- fill right-side publish status card

## 4. Query Task Steps

`GET /task/{taskId}/steps`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": [
    {
      "id": 101,
      "taskId": 21,
      "stepNo": 1,
      "stepName": "topic_plan",
      "stepStatus": "COMPLETED",
      "outputText": "节点输出",
      "metadataJson": "{\"stage\":\"CONTENT_TOPIC_PLAN\",\"degraded\":false}",
      "createTime": "2026-04-15 11:40:10.0"
    }
  ]
}
```

Frontend usage:
- restore full workflow timeline
- show each stage output independently
- support page refresh without losing intermediate results

## 5. Query Task History

`GET /task/history?limit=20`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": [
    {
      "taskId": 21,
      "taskCode": "ct_021",
      "topic": "企业级 AI Agent 编排平台",
      "platform": "公众号",
      "channel": "mock",
      "status": "COMPLETED",
      "currentStep": "COMPLETED",
      "title": "标题",
      "publishStatus": "DRAFT_SAVED",
      "createTime": "2026-04-15 11:40:00.0",
      "updateTime": "2026-04-15 11:41:00.0"
    }
  ]
}
```

Query params:
- `limit`: optional, default `20`, max `50`

Frontend usage:
- click “历史记录” button, then call this API
- render result in a modal list
- after user clicks one item, call:
  - `GET /task/{taskId}`
  - `GET /task/{taskId}/steps`
- use returned detail + steps to restore the full page

## 6. Frontend State Mapping

Suggested page state:

```ts
type ContentTaskState = {
  taskId: number
  taskCode: string
  executionMode: 'STRUCTURED_PLAN_EXECUTE'
  status: string
  currentStep: string
  title?: string
  outlineText?: string
  draftContent?: string
  finalContent?: string
  complianceResult?: string
  publishStatus?: string
  publishExternalId?: string
  publishExternalUrl?: string
  summaryText?: string
}
```

```ts
type ContentStepItem = {
  id: number
  taskId: number
  stepNo: number
  stepName: string
  stepStatus: string
  outputText: string
  metadataJson: string
  createTime: string
}
```

```ts
type ContentHistoryItem = {
  taskId: number
  taskCode: string
  topic: string
  platform: string
  channel: string
  status: string
  currentStep: string
  title?: string
  publishStatus?: string
  createTime: string
  updateTime: string
}
```

```ts
type ContentStreamEvent = {
  type: 'content_step' | 'content_complete' | 'content_error'
  taskId: number
  stepNo?: number
  stepName?: string
  status: string
  content: string
  completed: boolean
  timestamp: number
}
```

## 7. Recommended Frontend Flow

### New task flow
1. call `POST /task/create`
2. keep returned `taskId`
3. call `POST /task/execute/stream`
4. consume SSE events for live progress
5. after `content_complete`, call:
   - `GET /task/{taskId}`
   - `GET /task/{taskId}/steps`
6. render:
   - left: task config
   - center: title / outline / draft / final content
   - right: step timeline + publish result

### History dialog flow
1. click “历史记录”
2. call `GET /task/history?limit=20`
3. show dialog list
4. click one history item
5. call:
   - `GET /task/{taskId}`
   - `GET /task/{taskId}/steps`
6. close dialog and replace current page state with selected task data

## 8. Frontend Rendering Rules

1. SSE is incremental only
- do not treat SSE as final truth
- final truth is `detail + steps`

2. History restore must not recreate task
- when user opens history item, only call detail + steps
- do not call `/task/create`
- do not reconnect SSE automatically

3. Clear temporary stream state when switching history item
- clear current loading flag
- clear current temporary step buffer
- then hydrate page from selected history task

4. Use `stepName` to map content
- `outline` -> outline area
- `draft` -> draft area
- `polish` -> final content area
- `compliance` -> compliance area
- `publish_execute` -> publish result area
- `publish_summary` -> summary area

## 9. Current Publish Boundary

Current `channel` support:
- `mock`

Current publish behavior:
- draft only
- no real third-party platform integration

Current publish status:
- `DRAFT_SAVED`
- `BLOCKED`
- `NOT_EXECUTED`

## 10. Channel Config API

Base path: `/api/v1/content/channel`

### 10.1 Save Global Channel Config

`POST /config/save`

Request:

```json
{
  "channel": "juejin",
  "token": "your-juejin-token"
}
```

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "channel": "juejin",
    "channelName": "掘金",
    "authType": "token",
    "verifyStatus": "UNVERIFIED",
    "verifyMessage": "待验证",
    "status": 1
  }
}
```

Frontend usage:
- save token first
- after save, call verify endpoint

### 10.2 Query Global Channel Config

`GET /config/juejin`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "channel": "juejin",
    "channelName": "掘金",
    "authType": "token",
    "verifyStatus": "VERIFIED",
    "verifyMessage": "token 可用",
    "status": 1
  }
}
```

Frontend usage:
- page init restore current config state
- show verification badge

### 10.3 Verify Juejin Token

`POST /juejin/verify`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "channel": "juejin",
    "verified": true,
    "verifyStatus": "VERIFIED",
    "message": "token 可用"
  }
}
```

Frontend usage:
- click “验证身份”
- if `verified=true`, allow user to choose `channel=juejin`
- if false, keep publishing blocked

### 10.4 Query Publish Records

`GET /record/{taskId}`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": [
    {
      "id": 10,
      "taskId": 21,
      "channelCode": "juejin",
      "action": "save_draft",
      "status": "BLOCKED",
      "externalId": null,
      "externalUrl": null,
      "errorMessage": "官方公开 API 暂不支持文章发布",
      "createTime": "2026-04-15 15:10:00.0"
    }
  ]
}
```

Frontend usage:
- render publish attempt history in right-side panel or detail dialog

## 11. Juejin Boundary

Current juejin integration:
- global single-account config
- official token verification
- publish channel routing
- publish attempt recording

Current limitation:
- official public API found in this round supports token verification, but does not expose a stable public article publish API
- therefore current `channel=juejin` execution will be blocked with an explicit message after planning, rather than faking a publish success

## 12. Cnblogs MetaWeblog Draft Publish

Current `cnblogs` integration:
- global single-account config
- MetaWeblog XML-RPC credential verification
- draft publish only
- publish attempt recording

### 12.1 Save Cnblogs Config

`POST /api/v1/content/channel/config/save`

Request:

```json
{
  "channel": "cnblogs",
  "blogApp": "your_blog_app",
  "blogId": "your_blog_app",
  "username": "your_cnblogs_username",
  "token": "your_metaweblog_access_token",
  "endpoint": "https://rpc.cnblogs.com/metaweblog/your_blog_app"
}
```

Field notes:
- `blogApp`: blog path name, used by frontend display and default endpoint composition
- `blogId`: MetaWeblog `blogid`, usually same as `blogApp`
- `username`: cnblogs account username
- `token`: cnblogs backend generated MetaWeblog access token/password
- `endpoint`: optional; if empty backend uses `https://rpc.cnblogs.com/metaweblog/{blogId}`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "channel": "cnblogs",
    "channelName": "博客园",
    "authType": "metaweblog",
    "verifyStatus": "UNVERIFIED",
    "verifyMessage": "待验证",
    "status": 1
  }
}
```

### 12.2 Verify Cnblogs Config

`POST /api/v1/content/channel/cnblogs/verify`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "channel": "cnblogs",
    "verified": true,
    "verifyStatus": "VERIFIED",
    "message": "博客园 MetaWeblog 配置可用"
  }
}
```

Frontend usage:
- config first, then verify
- only allow choosing `channel=cnblogs` after `verifyStatus=VERIFIED`

### 12.3 Execute Cnblogs Draft Publish

Create content task with:

```json
{
  "topic": "企业级 AI Agent 编排平台",
  "platform": "博客园",
  "style": "专业",
  "keywords": "AI Agent,Java,DDD",
  "channel": "cnblogs"
}
```

Workflow behavior:
- `publish_plan` should produce `save_draft`
- `publish_execute` calls `metaWeblog.newPost`
- backend sends publish flag `false`
- result status is `DRAFT_SAVED`

Publish record example:

```json
{
  "id": 11,
  "taskId": 21,
  "channelCode": "cnblogs",
  "action": "save_draft",
  "status": "DRAFT_SAVED",
  "externalId": "123456",
  "externalUrl": null,
  "errorMessage": "博客园草稿已保存，草稿 ID=123456",
  "createTime": "2026-04-15 16:30:00.0"
}
```

Current boundary:
- only save draft
- no automatic public publish
- no Playwright cookie automation in this version

## 13. Dev.to Draft Publish

Current `devto` integration:
- global single-account config
- official API Key verification
- draft publish only
- publish attempt recording

### 13.1 Save Dev.to Config

`POST /api/v1/content/channel/config/save`

Request:

```json
{
  "channel": "devto",
  "token": "devto_api_key",
  "username": "your_devto_username",
  "endpoint": "https://dev.to"
}
```

Field notes:
- `channel`: fixed `devto`
- `token`: Dev.to API Key
- `username`: optional display field, recommended to fill
- `endpoint`: optional; current version uses it as Dev.to base URL, default `https://dev.to`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "channel": "devto",
    "channelName": "Dev.to",
    "authType": "api_key",
    "verifyStatus": "UNVERIFIED",
    "verifyMessage": "待验证",
    "status": 1
  }
}
```

### 13.2 Verify Dev.to Config

`POST /api/v1/content/channel/devto/verify`

Response:

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "channel": "devto",
    "verified": true,
    "verifyStatus": "VERIFIED",
    "message": "Dev.to API Key 可用"
  }
}
```

Frontend usage:
- save config first
- verify second
- only allow `channel=devto` after `verifyStatus=VERIFIED`

### 13.3 Execute Dev.to Draft Publish

Create content task with:

```json
{
  "topic": "企业级 AI Agent 编排平台",
  "platform": "Dev.to",
  "style": "专业",
  "keywords": "AI Agent,Java,DDD",
  "channel": "devto"
}
```

Workflow behavior:
- `publish_plan` should produce `save_draft`
- `publish_execute` calls `POST /api/articles`
- backend always sends `published=false`
- result status is `DRAFT_SAVED`

Publish record example:

```json
{
  "id": 12,
  "taskId": 21,
  "channelCode": "devto",
  "action": "save_draft",
  "status": "DRAFT_SAVED",
  "externalId": "321",
  "externalUrl": "https://dev.to/tester/devto-draft",
  "errorMessage": "Dev.to 草稿已保存，草稿 ID=321",
  "createTime": "2026-04-15 17:30:00.0"
}
```

Current boundary:
- only save draft
- no public publish in this version
- no draft list query in this version
