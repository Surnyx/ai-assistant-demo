# AI 智能助手平台 Demo

这是一个面向软件工程学生的简洁 AI 聊天项目。项目使用 Spring Boot、MyBatis-Plus、MySQL 和原生 HTML/CSS/JavaScript，实现了从登录到多轮 AI 对话的完整流程。

## 功能

- 固定测试账号登录，使用 Session 保存登录状态
- 新建、查看、切换和删除聊天会话
- 聊天消息持久化到 MySQL
- 查询最近 10 条消息构造多轮上下文
- 调用兼容 OpenAI Chat Completions 格式的大模型接口
- 默认 Mock 模式，无 API Key 也可以完整演示
- 原生前端，不依赖 Vue、React 或 Node.js 运行环境

## 技术栈

- Java 17
- Spring Boot 3.5.16
- Spring Web
- MyBatis-Plus 3.5.17
- MySQL 8
- Maven
- HTML、CSS、JavaScript

## 项目结构

```text
ai-assistant-demo/
├─ sql/                         数据库初始化脚本
├─ src/main/java/com/demo/aiassistant/
│  ├─ config/                   AI配置、Session拦截器等
│  ├─ controller/               接收HTTP请求
│  ├─ dto/                      请求和响应对象
│  ├─ entity/                   数据库实体
│  ├─ exception/                统一异常处理
│  ├─ mapper/                   MyBatis-Plus Mapper
│  ├─ service/                  业务接口
│  └─ service/impl/             业务实现和LLM调用
├─ src/main/resources/
│  ├─ db/schema.sql             部署时自动初始化三张表和测试账号
│  ├─ static/                   登录页和聊天页
│  ├─ application.yml           通用配置
│  └─ application-prod.yml      生产环境配置
├─ Dockerfile                   Railway 部署镜像
├─ start-demo.ps1               当前电脑一键启动脚本
└─ stop-demo.ps1                当前电脑停止脚本
```

## 当前电脑一键运行

在 PowerShell 中进入项目目录：

```powershell
cd D:\workspace\ai-assistant-demo
.\start-demo.ps1
```

脚本会依次启动便携 MySQL、执行可重复的初始化 SQL、构建项目并启动 Spring Boot。

浏览器访问：

```text
http://localhost:8080/
```

登录账号：

```text
test / 123456
```

停止项目：

```powershell
.\stop-demo.ps1
```

运行日志位于 `runtime/`，该目录不会提交到 Git。

## 在其他电脑或 IDEA 中运行

1. 安装 JDK 17、Maven 和 MySQL 8。
2. 执行 `sql/ai_assistant_demo.sql`。
3. 设置数据库环境变量，或修改 `application.yml` 中的默认值。
4. 在 IDEA 中运行 `AiAssistantApplication`。
5. 访问 `http://localhost:8080/`。

常用环境变量可参考 `.env.example`。项目不会自动读取 `.env` 文件，IDEA 用户应把这些变量配置到 Run Configuration 中。

## AI 配置

默认 Mock 模式：

```yaml
ai:
  mock: true
```

连接 DeepSeek 等兼容 OpenAI 格式的接口时，可以配置：

```text
AI_MOCK=false
AI_BASE_URL=https://api.deepseek.com
AI_API_KEY=你的API密钥
AI_MODEL=deepseek-flash
AI_MAX_TOKENS=800
```

API Key 只通过环境变量传入，不要提交到代码仓库。

## Railway 部署

项目使用同一个 Spring Boot 服务提供网页和 API，因此不需要配置 CORS。部署时新建一个 Railway MySQL 服务和一个连接 GitHub 仓库的应用服务；Dockerfile 会固定使用 Java 17 构建和运行。

应用服务需要设置：

```text
SPRING_PROFILES_ACTIVE=prod
MYSQLHOST=${{MySQL.MYSQLHOST}}
MYSQLPORT=${{MySQL.MYSQLPORT}}
MYSQLDATABASE=${{MySQL.MYSQLDATABASE}}
MYSQLUSER=${{MySQL.MYSQLUSER}}
MYSQLPASSWORD=${{MySQL.MYSQLPASSWORD}}
AI_BASE_URL=https://api.deepseek.com
AI_API_KEY=在Railway控制台中填写的新密钥
AI_MODEL=deepseek-flash
AI_MAX_TOKENS=800
```

生产配置会自动执行 `db/schema.sql`，创建数据表和 `test / 123456` 测试账号。端口读取 Railway 自动提供的 `PORT`，无需手工填写 Build Command 或 Start Command。

## 接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/conversations` | 新建会话 |
| GET | `/api/conversations` | 查询历史会话 |
| DELETE | `/api/conversations/{id}` | 删除会话及消息 |
| GET | `/api/conversations/{id}/messages` | 查询会话消息 |
| POST | `/api/chat` | 发送消息并获得AI回答 |

登录请求示例：

```json
{
  "username": "test",
  "password": "123456"
}
```

聊天请求示例：

```json
{
  "conversationId": 1,
  "message": "你好，请介绍一下 Spring Boot"
}
```

## 设计说明

- Controller 只处理请求与响应，业务逻辑集中在 Service。
- 数据访问统一放在 Mapper，使用 MyBatis-Plus 简化基础 CRUD。
- LLM HTTP 调用独立封装在 `LlmService`，没有写进 Controller。
- 所有会话操作都同时校验 `conversationId` 和当前登录用户，避免访问其他用户的会话。
- 删除会话时依靠数据库外键级联删除消息。
- 项目定位是可讲清楚、可演示的学生 Demo，因此没有加入微服务、Redis、消息队列或复杂权限系统。

## 测试

```powershell
mvn test
```

单元测试覆盖登录校验、Session 拦截、会话归属、多轮上下文顺序和 Mock AI 回复。
