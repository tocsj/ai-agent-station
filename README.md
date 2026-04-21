# AI Agent Station

企业级 AI Agent 工作台后端服务，当前包含简历评估、模拟面试、文档知识助手、内容自动化和审计工作台等能力。

## 1. 环境要求

- JDK 17
- Maven 3.8+
- Docker Desktop / Docker Engine
- 一个兼容 OpenAI 协议的模型服务 Key，默认示例使用 DashScope 兼容模式

前端是单独项目，本仓库只包含后端和后端运行所需的数据库初始化资料。

## 2. 拉取代码

```bash
git clone -b public-clean https://github.com/tocsj/ai-agent-station.git
cd ai-agent-station
```

## 3. 启动基础依赖

项目运行需要 MySQL 和 PostgreSQL + PGVector。

```bash
cd docs/dev-ops/tmp
docker compose up -d
```

默认端口：

- MySQL: `127.0.0.1:13306`
- PostgreSQL/PGVector: `127.0.0.1:5432`

初始化脚本位置：

- MySQL: `docs/dev-ops/mysql/sql/ai-agent-station-study.sql`
- PGVector: `docs/dev-ops/rag-file/pgvector-init.sql`

如果你之前启动过同名 compose，想重新导入初始化数据，需要先清空 volume：

```bash
cd docs/dev-ops/tmp
docker compose down -v
docker compose up -d
```

## 4. 配置模型 Key

为了避免泄露密钥，仓库里的 SQL 和配置文件不包含真实 Key。

### 4.1 配置后端环境变量

Windows PowerShell:

```powershell
$env:DASHSCOPE_API_KEY="你的模型服务Key"
```

macOS / Linux:

```bash
export DASHSCOPE_API_KEY="你的模型服务Key"
```

### 4.2 配置数据库里的模型 API Key

MySQL 初始化脚本里的 `ai_client_api.api_key` 是占位值。启动 MySQL 后，需要写入你自己的 Key。

```bash
docker exec -it ai-agent-station-mysql mysql -uroot -p123456 ai-agent-station
```

进入 MySQL 后执行：

```sql
UPDATE ai_client_api
SET api_key = '你的模型服务Key'
WHERE api_id = '1003';
```

如果你没有 `1001`、`1002` 对应服务的 Key，可以保持占位值。当前主要业务客户端使用 `1003` 下的通义兼容模型配置。

## 5. 启动后端

回到仓库根目录：

```bash
mvn -pl ai-agent-station-app -am spring-boot:run
```

默认后端端口：

```text
http://localhost:8091
```

默认启用 `dev` profile，配置文件在：

```text
ai-agent-station-app/src/main/resources/application-dev.yml
```

## 6. 前端启动

前端不在本仓库中，需要单独拉取前端项目并启动。前端服务需要把 API 地址指向：

```text
http://localhost:8091
```

## 7. 初始化数据边界

为了方便公开仓库和同学本地运行，初始化数据做了脱敏和裁剪：

- MySQL 保留完整表结构。
- MySQL 只保留 Agent、Client、Model、Prompt、Advisor 等基础配置数据。
- MySQL 不导出简历、文档、历史任务、审计日志、发布凭证等业务数据。
- PGVector 保留向量表结构。
- PGVector 只导出 Java 后端岗位标准知识和对应向量。
- PGVector 不导出简历向量和文档向量。
- Nginx、Grafana、Kibana、Logstash、Prometheus 等当前运行不需要的资料已移除。

## 8. 常见问题

### 模型调用失败

检查两处 Key：

- 环境变量 `DASHSCOPE_API_KEY`
- MySQL 表 `ai_client_api` 中 `api_id = 1003` 的 `api_key`

### 数据没有重新初始化

Docker 官方镜像只会在空 volume 时执行 `/docker-entrypoint-initdb.d` 初始化脚本。需要重新导入时执行：

```bash
cd docs/dev-ops/tmp
docker compose down -v
docker compose up -d
```

### 发布文章失败

公开初始化数据不包含 Dev.to、博客园、掘金等发布平台凭证。需要在页面或数据库中重新配置发布渠道凭证后才能真正发布。
