# Tare - 数据推送管理系统

## How to Run

### Docker启动
```bash
# 使用 Docker Compose 启动
docker compose up --build -d

# 查看日志
docker compose logs -f

# 停止服务
docker compose down
```
### 本地开发

> **若无 Maven/Java 开发环境**：可直接使用 [Docker 启动](#docker启动)，通过 `docker compose up --build -d` 一键运行，避免环境差异带来的问题。

**前置条件**（请确保已安装）:
- **JDK 17**（`java -version` 可验证）
- **Maven**（`mvn -v` 可验证）
- **Node.js 18+**（`node -v` 可验证，用于前端）

后端:
```bash
cd backend
mvn spring-boot:run
```

前端:
```bash
cd frontend
npm install
npm run dev
```
访问地址: http://localhost:5173 （Vite 默认端口，见 vite.config.js）


## Services

| 服务 | 端口 | 说明 |
|------|------|------|
| Frontend (Docker) | 8081 | Vue3 + Ant Design Vue 前端界面（Docker Nginx 代理） |
| Frontend (本地开发) | 5173 | Vite dev server 前端开发服务器 |
| Backend | 8080 | Spring Boot 后端 API |

访问地址: Docker 部署 `http://localhost:8081` / 本地开发 `http://localhost:5173`

### 日志与排错

- **后端日志**：`backend/logs/tare-backend.log`（相对 backend 目录）；支持滚动，单文件最大 10MB，保留 30 天
- **查看日志**：`tail -f backend/logs/tare-backend.log` 或 `docker compose logs -f`（Docker 部署时）
- **日志关联 ID**：每个 HTTP 请求与定时任务执行均注入 `traceId` 到 MDC，日志 pattern 输出 `[%X{traceId}]`，便于跨调用排错；响应头 `X-Trace-Id` 可透传用于链路追踪

## 测试账号

本系统无需登录，直接访问即可使用。

## 题目内容

使用springboot + Ant Design of Vue，为我开发一款web应用，后端数据先不使用数据库，数据先使用模拟数据，它的功能介绍如下 

## 产品概述 

- 产品名称：tare 

- 功能概述：一个推送接口、数据源接口数据管理，以及数据源接口返回数据和推送接口请求数据绑定功能，并能定时调用数据源接口将获取到的数据推送到推送接口对应的系统中 

- 应用平台：pc端 

## 核心功能模块 

### 1. 推送接口管理 

- 新增接口包括接口访问地址、接口的头、接口请求方式，管理接口的 

- 接口请求参数内容管理 

### 2. 数据源接口配置管理 

- 新增接口包括接口访问地址、接口的头、接口请求方式 
- 接口返回数据管理 
- 接口数据后置处理 

### 3. 推送接口和数据源接口数据绑定 

- 推送接口请求参数和数据原接口返回数据绑定 

- 无绑定的数据可以设置默认值 

### 4. 定时数据源接口，并调用绑定的推送接口 

- 调用数据源接口获取数据，将数据通过绑定关系设置为对应的推送接口的请求数据，并调用推送接口 

- 数据源接口和推送接口的请求参数，请求结果的调用日志记录

---

## 项目介绍

Tare 是一个数据推送管理系统，用于配置数据源接口与推送接口的绑定关系，支持定时任务自动执行数据同步。

## 功能特性

### 1. 推送接口管理
- 配置推送目标接口（URL、请求方法、请求头）
- 定义请求参数（名称、类型、是否必填、默认值）
- 支持 GET/POST/PUT/DELETE 请求方法

### 2. 数据源接口管理
- 配置数据源接口（URL、请求方法、请求头、请求体）
- 定义返回数据字段（JSON路径、字段名称、类型）
- 数据后置处理：支持简单指令和JavaScript脚本对返回数据进行转换

### 3. 绑定配置
- 将数据源接口与推送接口进行绑定
- 配置字段映射（数据源字段 → 推送参数）
- 支持设置默认值
- Cron定时任务：支持标准Cron表达式，自动定时执行

### 4. 执行日志
- 记录每次执行的详细信息
- 包含数据源请求/响应、推送请求/响应
- 执行统计（成功率、平均耗时）

### 5. 数据持久化说明（符合题目约束）

- **当前实现**：绑定配置与执行日志存放于内存（`MockDataStore`），服务重启即丢失，与「先不使用数据库」的题目约束一致
- **后续迭代**：建议引入存储（文件 / 轻量 DB）以保留配置与执行历史，适用于生产形态

### 6. 离线可验证
- 当数据源或推送接口的 URL 包含 `example.com` 时，将使用内置 Mock 响应，不发起真实网络请求
- 便于在无网络环境下验证定时执行流程（Mock 数据结构与 MockDataStore 示例一致）

## 后置处理脚本

数据源接口支持后置处理，用于对返回数据进行转换：

**JavaScript 后置处理的运行前提**：项目依赖 `org.openjdk.nashorn` 作为 JavaScript 引擎（Nashorn 自 Java 15 起已从 JDK 默认移除）。若 Nashorn 不可用，会降级到内置声明式指令；若仅使用简单指令（如 `extract:`、`map:`），无需 Nashorn。**若需更强 JS 能力**，建议集成 GraalVM JavaScript（graal-js）或将复杂逻辑改为声明式规则。

简单指令:
```
extract:data.items      # 提取指定路径的数据
map:oldField->newField  # 字段重命名
```

JavaScript脚本:
```javascript
var result = data.items.filter(function(item) {
  return item.status === 'active';
});
```

## Cron表达式示例

| 表达式 | 说明 |
|--------|------|
| `0 0/5 * * * ?` | 每5分钟执行 |
| `0 0 * * * ?` | 每小时执行 |
| `0 0 0 * * ?` | 每天零点执行 |
| `0 0 9 * * MON-FRI` | 工作日9点执行 |

## 测试

### 后端测试

```bash
cd backend
mvn test
```

测试覆盖：
- 单元测试：`PostProcessorService`、`ExecutionService`、`MockDataStore`
- 集成测试：`DataSourceController`、`BindingController`、`ExecutionLogController`（CRUD、createdAt 保留、执行触发等）

### 前端测试

```bash
cd frontend
npm install
npm run test
```

测试覆盖：API 模块、App 组件等。

### 前端代码检查

```bash
cd frontend
npm run lint
```

## 配置

### 前端环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `VITE_API_BASE_URL` | 后端 API 地址 | `/api` |

生产环境可在构建时通过 `.env.production` 或 CI 环境变量设置。

### 后端 CORS 配置

| 变量/配置 | 说明 | 默认值 |
|----------|------|--------|
| `CORS_ALLOWED_ORIGINS` / `app.cors.allowed-origins` | 允许的跨域来源，多个用逗号分隔 | `*`（开发） |

**生产环境**：当前默认放开任意来源，适合开发；生产需收敛。建议在 `application-production.yml` 或通过环境变量 `CORS_ALLOWED_ORIGINS` 限定具体前端域名，可参考 `backend/src/main/resources/application-production.yml.example`。

## API文档

详细API文档请参考 [docs/API.md](docs/API.md)

## 技术栈

- 后端: Spring Boot 3.2 + Java 17 + Spring Validation
- 前端: Vue 3 + Ant Design Vue 4 + Vite 5 + Axios
- 容器化: Docker + Docker Compose

## 项目结构

```
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/com/tare/
│   │   ├── controller/     # REST API 控制器
│   │   ├── dto/            # 数据传输对象 + 参数校验
│   │   ├── exception/      # 全局异常处理
│   │   ├── model/          # 数据模型
│   │   ├── service/        # 业务逻辑
│   │   ├── store/          # 模拟数据存储
│   │   └── validator/      # 自定义校验器
│   ├── Dockerfile
│   └── pom.xml
├── frontend/               # Vue3 前端
│   ├── src/
│   │   ├── api/           # API 调用 + 错误处理
│   │   ├── router/        # 路由配置
│   │   └── views/         # 页面组件
│   ├── Dockerfile
│   └── package.json
├── docs/
│   └── API.md             # API 文档
├── docker-compose.yml
└── README.md
```


