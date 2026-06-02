# OJ在线评测系统 — 系统设计文档

## 目录

1. [引言](#1-引言)
   1.1 [文档编写目的与预期价值](#11-文档编写目的与预期价值)
   1.2 [系统设计范围与边界](#12-系统设计范围与边界)
   1.3 [目标读者群体](#13-目标读者群体)
   1.4 [关键术语定义](#14-关键术语定义)
   1.5 [参考资料](#15-参考资料)
   1.6 [遵循的标准与约定](#16-遵循的标准与约定)
2. [系统概述](#2-系统概述)
   2.1 [系统定位与功能描述](#21-系统定位与功能描述)
   2.2 [核心业务流程](#22-核心业务流程)
   2.3 [关键技术选型依据](#23-关键技术选型依据)
3. [架构设计](#3-架构设计)
   3.1 [架构模式](#31-架构模式)
   3.2 [分层设计](#32-分层设计)
   3.3 [模块划分与依赖关系](#33-模块划分与依赖关系)
   3.4 [组件交互设计](#34-组件交互设计)
4. [数据设计](#4-数据设计)
   4.1 [数据模型总览](#41-数据模型总览)
   4.2 [数据库选型](#42-数据库选型)
   4.3 [核心表结构设计](#43-核心表结构设计)
   4.4 [数据流设计](#44-数据流设计)
5. [接口设计](#5-接口设计)
   5.1 [接口架构](#51-接口架构)
   5.2 [核心接口定义](#52-核心接口定义)
   5.3 [接口规范](#53-接口规范)
6. [安全设计](#6-安全设计)
   6.1 [身份认证机制](#61-身份认证机制)
   6.2 [权限控制策略](#62-权限控制策略)
   6.3 [数据加密方案](#63-数据加密方案)
   6.4 [安全防护措施](#64-安全防护措施)
7. [部署设计](#7-部署设计)
   7.1 [部署架构](#71-部署架构)
   7.2 [环境配置要求](#72-环境配置要求)
   7.3 [资源需求估算](#73-资源需求估算)
8. [性能设计](#8-性能设计)
   8.1 [性能指标要求](#81-性能指标要求)
   8.2 [性能瓶颈分析](#82-性能瓶颈分析)
   8.3 [性能优化策略](#83-性能优化策略)
   8.4 [性能测试方法](#84-性能测试方法)
9. [扩展性设计](#9-扩展性设计)
   9.1 [扩展策略](#91-扩展策略)
   9.2 [模块解耦设计](#92-模块解耦设计)
10. [测试策略](#10-测试策略)

***

## 1. 引言

### 1.1 文档编写目的与预期价值

本文档旨在：

1. 规范OJ在线评测系统的架构设计与实现，为开发团队提供统一的技术指导
2. 明确系统的功能边界与非功能性需求，确保各模块设计目标一致
3. 为后续维护、扩展、升级提供技术参考，降低系统重构成本
4. 建立技术知识库，帮助新成员快速熟悉系统架构与核心业务流程

### 1.2 系统设计范围与边界

**系统范围：**

- 在线代码提交与自动评测
- 题目管理（含AI辅助生成）
- 比赛组织与管理
- 团队协作与管理
- 用户社区（文章、题解）
- 错题本与学习记录
- 系统监控与告警

**系统边界：**

- 判题服务与主应用通过HTTP接口解耦，独立部署
- AI服务使用第三方OpenAI兼容API，系统不负责模型训练
- 系统不负责代码版权保护，代码共享功能由用户自行决定

### 1.3 目标读者群体

- **开发人员**：了解系统架构与接口规范，进行功能开发与维护
- **运维人员**：了解部署架构与资源需求，进行系统部署与监控
- **技术负责人**：了解系统技术选型与扩展性设计，进行技术决策
- **测试人员**：了解核心业务流程，设计测试用例与验收标准

### 1.4 关键术语定义

| 术语     | 定义                              |
| ------ | ------------------------------- |
| OJ     | Online Judge，在线评测系统             |
| AC     | Accepted，指程序通过所有测试用例            |
| WA     | Wrong Answer，指程序输出结果与标准答案不一致    |
| TLE    | Time Limit Exceeded，指程序运行超时     |
| MLE    | Memory Limit Exceeded，指程序内存使用超限 |
| RE     | Runtime Error，指程序运行时发生错误        |
| CE     | Compilation Error，指程序编译失败       |
| SE     | System Error，指系统内部发生错误          |
| AI辅助生成 | 指利用AI模型自动生成题目、题解、测试数据的功能        |
| 软删除    | 指在数据库中标记为已删除而非物理删除，便于数据恢复       |
| 沙箱     | 指用于安全隔离代码运行的环境，限制系统调用与资源使用      |
| 负载均衡   | 指在多台判题机之间分配判题任务，提高系统吞吐量         |

### 1.5 参考资料

- Spring Boot官方文档
- Spring Data JPA官方文档
- MySQL 8.0官方文档
- Docker Compose文档
- OpenAI API文档

### 1.6 遵循的标准与约定

- **代码规范**：遵循阿里巴巴Java开发手册
- **接口规范**：遵循RESTful API设计原则
- **数据库命名**：表名使用下划线分隔，小写字母
- **日志规范**：遵循SLF4J + Logback规范，日志级别合理设置
- **Git规范**：遵循Git Flow分支管理策略

***

## 2. 系统概述

### 2.1 系统定位与功能描述

**系统定位：**
面向ACM/ICPC竞赛训练的在线评测平台，提供题目练习、比赛组织、团队协作、AI辅助出题等功能，支持多种编程语言的自动评测。

**核心功能：**

1. **用户管理**：注册登录、权限控制、个人档案
2. **题目模块**：题目浏览、代码提交、AI辅助生成题目与题解
3. **比赛模块**：比赛创建、实时排名、封榜、比赛提交
4. **团队模块**：团队创建、成员管理、团队比赛
5. **社区模块**：文章发布、题解交流、评论互动
6. **学习模块**：错题本、学习记录、复习提醒
7. **监控模块**：系统监控、性能告警、日志分析
8. **AI模块**：AI辅助出题、测试数据生成、4步审核流程

### 2.2 核心业务流程

#### 2.2.1 代码提交与评测流程

```
用户提交代码
    │
    ▼
验证登录状态
    │
    ▼
检查提交频率（10秒内不允许重复提交）
    │
    ▼
验证代码格式（长度限制、主程序入口）
    │
    ▼
创建Solution记录（状态=PENDING）
    │
    ▼
JudgeService负载均衡选择判题机
    │
    ▼
计算时间/内存倍率
    │
    ▼
发送到Python Judger（异步）
    │
    ▼
判题机编译代码，逐个运行测试用例
    │
    ▼
返回结果，POST到/judge/callback
    │
    ▼
更新Solution（状态、时间、内存）
    │
    ▼
更新题目统计（提交数、通过数）
    │
    ▼
更新用户档案（提交数、通过数、分数）
    │
    ▼
自动记录错题（若状态非AC）
```

#### 2.2.2 AI辅助出题4步流程

```
步骤1: AI生成题目 + 参考代码
    │
    ▼
管理员审核编辑（可修改题目内容、参考代码）
    │
    ▼
步骤2: AI生成测试输入数据
    │
    ▼
管理员审核测试输入（可编辑、删除、添加用例）
    │
    ▼
步骤3: 运行参考代码获取输出
    │
    ▼
管理员确认运行结果（必须全部AC才能进入下一步）
    │
    ▼
步骤4: 确认创建测试文件
    │
    ▼
写入.in/.out文件到文件系统
    │
    ▼
题目正式发布
```

### 2.3 关键技术选型依据

| 技术选型                      | 选型依据                                         |
| ------------------------- | -------------------------------------------- |
| Spring Boot 2.2.3         | 成熟稳定的后端框架，生态完善，适合快速开发企业级应用                   |
| Thymeleaf + Vue.js        | 前后端不分离架构，SEO友好，部署简单，适合中小型项目                  |
| MySQL 8.0.19              | 开源关系型数据库，性能优秀，ACID特性支持事务                     |
| Redis                     | 高性能键值存储，用于缓存和Session管理                       |
| Python + Flask + \_judger | Python生态完善，Flask轻量灵活，\_judger提供seccomp沙箱安全隔离 |
| OpenAI兼容API (gpt-4o-mini) | 模型能力强，API易用，支持多种调用方式，成本可控                    |
| Docker Compose            | 一键部署，环境一致性，便于扩展                              |

***

## 3. 架构设计

### 3.1 架构模式

系统采用**分层架构 + 服务解耦**模式：

```
┌───────────────────────────────────────────────────────────┐
│                     表现层 (Presentation)                   │
│  Thymeleaf模板 + Vue.js 2.x + Semantic UI + KaTeX         │
└───────────────────────────────────┬───────────────────────┘
                                    │
┌───────────────────────────────────▼───────────────────────┐
│                  控制层 (Controller)                      │
│  18个控制器，处理HTTP请求，参数验证，调用Service          │
└───────────────────────────────────┬───────────────────────┘
                                    │
┌───────────────────────────────────▼───────────────────────┐
│                   服务层 (Service)                         │
│  17个Service，核心业务逻辑，事务控制                      │
└───────────────────────────────────┬───────────────────────┘
                                    │
┌───────────────────────────────────▼───────────────────────┐
│                  数据访问层 (Repository)                   │
│  29个Repository，Spring Data JPA封装                      │
└───────────────────────────────────┬───────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
┌───────▼─────────┐       ┌─────────▼─────────┐     ┌───────▼─────────┐
│   MySQL 8.0    │       │   Python Judger  │     │   Redis         │
│   数据库       │       │   判题服务       │     │   缓存/Session  │
└─────────────────┘       └───────────────────┘     └─────────────────┘
```

### 3.2 分层设计

#### 3.2.1 表现层

**职责：**

- 页面渲染（Thymeleaf）
- 前端交互（Vue.js）
- 用户输入验证

**实现：**

- 页面路由通过Controller返回Thymeleaf模板
- 数据API通过`/api/*`前缀返回JSON
- 数学公式渲染使用KaTeX
- Markdown渲染使用marked.js

#### 3.2.2 控制层

**职责：**

- HTTP请求处理
- 参数验证
- 调用Service
- 异常处理
- Session管理

**实现：**

- 18个Controller，按功能模块划分
- 使用`@RestController`注解处理API请求
- 使用`@Controller`注解处理页面请求
- 拦截器在Controller之前执行权限检查

#### 3.2.3 服务层

**职责：**

- 核心业务逻辑
- 事务控制（`@Transactional`）
- 调用Repository
- 调用外部服务（Judge、AI）

**实现：**

- 17个Service，按功能模块划分
- JudgeService负责判题流程
- AIService负责AI调用
- JudgeRunService负责代码运行

#### 3.2.4 数据访问层

**职责：**

- 数据库访问
- 查询构建
- 关系映射

**实现：**

- 29个Repository，继承`JpaRepository`
- 自定义查询使用`@Query`注解
- 软删除通过Repository方法实现

### 3.3 模块划分与依赖关系

```
┌─────────────────────────────────────────────────────────────────────┐
│                      主应用模块 (Spring Boot)                         │
├─────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌───────────────────┐   │
│  │ 用户模块       │  │ 题目模块       │  │ 比赛模块        │   │
│  │ └─UserService  │  │ └─ProblemService│  │ └─ContestService  │   │
│  └─────────────────┘  └─────────────────┘  └───────────────────┘   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌───────────────────┐   │
│  │ 团队模块       │  │ 社区模块       │  │ 学习模块        │   │
│  │ └─TeamService  │  │ └─ArticleService│  │ └─ErrorService   │   │
│  └─────────────────┘  └─────────────────┘  └───────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ AI模块                                                      │  │
│  │ └─AIService + AIWorkflowController + JudgeRunService        │  │
│  └─────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
           │                          │                     │
┌──────────▼──────────┐      ┌────────▼────────┐    ┌─────▼──────┐
│  MySQL 8.0数据库   │      │ Python Judger  │    │ Redis      │
└─────────────────────┘      └─────────────────┘    └────────────┘
```

**依赖关系：**

- 所有模块依赖数据访问层（Repository）
- AI模块依赖JudgeRunService（代码运行）
- 题目模块、比赛模块依赖JudgeService（判题）
- 监控模块依赖所有模块的运行状态

### 3.4 组件交互设计

#### 3.4.1 主应用与判题机交互

```
主应用 (JudgeService)
    │
    ├─ POST /judge (异步) → 判题机
    │                          └─ 编译 + 运行 + 返回
    │
    │ POST /judge/callback ← 判题机 (异步回调)
    │
    └─ POST /run (同步) → 判题机
        └─ 直接返回运行结果
```

#### 3.4.2 主应用与AI API交互

```
AIService.callLLM()
    │
    ├─ 尝试 /responses (Gemini格式)
    │   └─ 失败 → 自动fallback
    │
    ├─ 尝试 /chat/completions (OpenAI格式)
    │   └─ 失败 → 自动fallback
    │
    └─ 尝试 /completions (传统格式)
        └─ 失败 → 抛出异常
```

#### 3.4.3 拦截器与Controller交互

```
HTTP请求
    │
    ▼
UnavailableInterceptor (维护模式检查)
    │
    ▼
LoginViewInterceptor/LoginInterceptor (登录检查)
    │
    ▼
TeacherCheckInterceptor (教师权限检查)
    │
    ▼
AdminCheckInterceptor (管理员权限检查)
    │
    ▼
ContestInterceptor (比赛访问检查)
    │
    ▼
TeamInterceptor (团队权限检查)
    │
    ▼
Controller方法
```

***

## 4. 数据设计

### 4.1 数据模型总览

```
User ──1:1── UserProfile
  │
  ├──1:1── Teacher (privilege: 0=TEACHER, 1=ADMIN)
  │
  ├──1:N── Solution ──N:1── Problem
  │                    └──N:1── Contest (可空)
  │
  ├──1:N── Article ──1:N── ArticleComment
  │
  ├──1:N── Analysis ──N:1── Problem
  │              └──1:N── AnalysisComment
  │
  ├──1:N── ErrorRecord ──N:1── Problem
  │              └──N:1── ErrorCategory
  │
  ├──1:N── Teammate ──N:1── Team ──1:N── Contest
  │
  └──1:N── AIGeneration

Problem ──M:N── Tag
        └──1:N── ContestProblem ──N:1── Contest

Contest ──1:N── ContestComment
       ──1:N── ContestUserCompletion
```

### 4.2 数据库选型

| 数据库                         | 选型依据                                |
| --------------------------- | ----------------------------------- |
| MySQL 8.0.19                | 开源关系型数据库，性能优秀，ACID支持事务，社区活跃，适合中小型项目 |
| HikariCP连接池                 | Spring Boot默认连接池，性能优秀，最大连接数10       |
| Spring Data JPA + Hibernate | ORM框架，简化数据访问，自动生成SQL                |

**配置：**

- 端口：13306（Docker）
- 数据库名：oj
- 字符集：utf8mb4
- 隔离级别：READ\_COMMITTED

### 4.3 核心表结构设计

| 表名               | 说明      | 关键字段                                                                                                                                                                                                                                                    |
| ---------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| user             | 用户      | id, username(唯一), password(BCrypt), name, email, createtime                                                                                                                                                                                             |
| user\_profile    | 用户档案    | id, user\_id, score, accepted, submitted                                                                                                                                                                                                                |
| teacher          | 教师/管理员  | id, user\_id, privilege(0=TEACHER, 1=ADMIN)                                                                                                                                                                                                             |
| problem          | 题目      | id, title(唯一), description, input, output, sample\_input, sample\_output, hint, source, time\_limit, memory\_limit, status(DRAFT/PENDING/APPROVED/REJECTED), score, submitted, accepted                                                                 |
| tag              | 标签      | id, name(唯一), score                                                                                                                                                                                                                                     |
| problem\_tag     | 题目-标签关联 | problem\_id, tag\_id                                                                                                                                                                                                                                    |
| solution         | 提交记录    | id, user\_id, problem\_id, contest\_id(可空), language, source(TEXT), submit\_time, ip, time, memory, length, result, share, info(TEXT), case\_number                                                                                                     |
| contest          | 比赛      | id, title, description, privilege(public/team/private), password, start\_time, end\_time, create\_time, pattern(acm), freeze\_rank, status(DRAFT/PENDING/APPROVED/REJECTED), user\_id(创建者), team\_id(可空)                                                |
| contest\_problem | 比赛-题目   | id, contest\_id, problem\_id, temp\_id, temp\_title(TEXT), submitted, accepted                                                                                                                                                                          |
| team             | 团队      | id, name(唯一), description(TEXT), attend(public/private), create\_time, user\_id(创建者)                                                                                                                                                                    |
| teammate         | 团队成员    | id, team\_id, user\_id, level(0=MASTER, 1=MANAGER, 2=MEMBER)                                                                                                                                                                                            |
| ai\_generation   | AI生成记录  | id, type(problem/analysis/solution), model, prompt(TEXT), generated\_content(TEXT), evaluation(TEXT), difficulty, status(PENDING/PROCESSING/COMPLETED/FAILED), priority, create\_time, complete\_time, cost, response\_time, user\_id, deleted(BOOLEAN) |
| article          | 文章      | id, title(1-50字符), text(TEXT, 15-5000字符), post\_time, user\_id                                                                                                                                                                                          |
| analysis         | 题解      | id, text(TEXT), user\_id, problem\_id, post\_time                                                                                                                                                                                                       |
| error\_record    | 错题记录    | id, user\_id, problem\_id, solution\_id, category\_id(可空), error\_type, error\_message(TEXT), create\_time, last\_review\_time, review\_count, difficulty, is\_marked, is\_resolved                                                                     |
| system\_monitor  | 系统监控    | id, monitor\_time, cpu\_usage, memory\_usage, disk\_usage, active\_threads, http\_requests, avg\_response\_time, error\_count, status, details(TEXT)                                                                                                    |
| error\_alarm     | 告警      | id, alarm\_time, alarm\_type, alarm\_message(TEXT), status(PENDING), priority(HIGH), details(TEXT), solution(TEXT), handle\_time, handler                                                                                                               |

### 4.4 数据流设计

#### 4.4.1 提交评测数据流

```
用户浏览器
    │ (POST /api/problems/{id}/submit)
    ▼
ProblemController
    │ (参数: source, language)
    ▼
JudgeService.submitCode()
    │ (构建SubmitCode对象)
    ▼
RESTService.postJson() → Python Judger /judge
    │
    ▼
Python Judger: 编译 → 运行测试用例
    │
    ▼
POST /judge/callback ← Judger
    │
    ▼
JudgeController.callback()
    │
    ├─ 更新Solution表
    ├─ 更新Problem表统计
    ├─ 更新UserProfile表统计
    └─ 插入ErrorRecord表（若失败）
```

#### 4.4.2 AI出题数据流

```
管理员浏览器
    │ (POST /api/ai/workflow/async-generate)
    ▼
AIWorkflowController
    │
    ▼
AIService.generateProblem() → AI API
    │
    ▼
返回JSON → 解析 → AIGeneration表
    │
    ▼
前端 /draft-problem/{id}
    │
    ▼
步骤2: 生成测试输入 → AI API
    │
    ▼
前端 /generate-testdata
    │
    ▼
步骤3: 运行参考代码 → Judger /run
    │
    ▼
前端 /run-solution
    │
    ▼
步骤4: 确认创建文件 → 写入文件系统
    │
    ▼
前端 /confirm-testdata
    │
    ▼
Problem表正式发布
```

***

## 5. 接口设计

### 5.1 接口架构

系统采用**RESTful + 页面路由**双层接口架构：

```
┌───────────────────────────────────────────────────────────┐
│                      用户浏览器                             │
└───────────────────────────────────┬───────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
┌───────▼───────────┐    ┌──────────▼──────────┐   ┌──────────▼──────────┐
│   页面路由         │    │   数据API (/api/*) │   │   判题回调         │
│   (/problems, etc)│    │   返回JSON         │   │   /judge/callback  │
│   返回Thymeleaf   │    │   RestfulResult    │   │   无需认证         │
│   模板            │    │   统一格式         │   │                    │
└───────────────────┘    └─────────────────────┘   └─────────────────────┘
```

### 5.2 核心接口定义

#### 5.2.1 用户接口

| 接口     | 方法   | 路径                    | 说明       |
| ------ | ---- | --------------------- | -------- |
| 用户注册   | POST | /register             | 用户注册     |
| 用户登录   | POST | /login                | 用户登录     |
| 用户登出   | GET  | /logout               | 用户登出     |
| 获取当前用户 | GET  | /api/user/session     | 获取当前登录用户 |
| 获取用户档案 | GET  | /api/user/{id}        | 获取用户档案   |
| 更新用户信息 | POST | /api/user/update/{id} | 更新用户信息   |

#### 5.2.2 题目接口

| 接口     | 方法   | 路径                          | 说明       |
| ------ | ---- | --------------------------- | -------- |
| 获取题目列表 | GET  | /api/problems               | 分页获取题目列表 |
| 获取题目详情 | GET  | /api/problems/{id}          | 获取题目详情   |
| 提交代码   | POST | /api/problems/{id}/submit   | 提交代码评测   |
| 获取提交记录 | GET  | /api/status                 | 分页获取提交记录 |
| 获取题解   | GET  | /api/problems/analysis/{id} | 获取题目题解   |

#### 5.2.3 比赛接口

| 接口     | 方法   | 路径                              | 说明         |
| ------ | ---- | ------------------------------- | ---------- |
| 获取比赛列表 | GET  | /api/contest                    | 获取比赛列表     |
| 获取比赛详情 | GET  | /api/contest/{id}               | 获取比赛详情     |
| 比赛签到   | POST | /api/contest/gate/{id}          | 比赛签到（密码验证） |
| 比赛提交   | POST | /api/contest/submit/{pid}/{cid} | 比赛内代码提交    |
| 获取比赛排名 | GET  | /api/contest/ranklist/{id}      | 获取比赛排名     |

#### 5.2.4 AI接口

| 接口       | 方法     | 路径                                              | 说明            |
| -------- | ------ | ----------------------------------------------- | ------------- |
| 获取配额信息   | GET    | /api/ai/quota                                   | 获取AI配额信息      |
| 异步生成题目   | POST   | /api/ai/workflow/async-generate                 | 异步生成题目（含参考代码） |
| 获取异步生成结果 | GET    | /api/ai/workflow/async-result/{asyncId}         | 获取异步生成结果      |
| 获取草稿题目   | GET    | /api/ai/workflow/draft-problem/{generationId}   | 获取草稿题目        |
| 编辑草稿题目   | POST   | /api/ai/workflow/edit-draft/{generationId}      | 编辑草稿题目        |
| 提交审核     | POST   | /api/ai/workflow/submit-for-review/{problemId}  | 提交审核          |
| 生成测试输入数据 | POST   | /api/ai/workflow/generate-testdata              | 生成测试输入数据      |
| 运行参考代码   | POST   | /api/ai/workflow/run-solution                   | 运行参考代码获取输出    |
| 确认创建测试文件 | POST   | /api/ai/workflow/confirm-testdata               | 确认创建测试文件      |
| 加载已有测试数据 | GET    | /api/ai/workflow/testdata/{problemId}           | 加载已有测试数据      |
| 更新测试用例   | PUT    | /api/ai/workflow/testdata/{problemId}/{caseNum} | 更新测试用例        |
| 删除测试用例   | DELETE | /api/ai/workflow/testdata/{problemId}/{caseNum} | 删除测试用例        |
| 删除生成记录   | DELETE | /api/ai/generations/{id}                        | 软删除生成记录       |

#### 5.2.5 管理接口

| 接口     | 方法     | 路径                              | 说明       |
| ------ | ------ | ------------------------------- | -------- |
| 审核题目   | POST   | /api/admin/problem/approve/{id} | 审核通过题目   |
| 拒绝题目   | POST   | /api/admin/problem/reject/{id}  | 拒绝题目     |
| 添加标签   | POST   | /api/admin/tag/add              | 添加标签     |
| 设置教师   | POST   | /api/admin/user/teacher/{id}    | 设置用户为教师  |
| 取消教师   | DELETE | /api/admin/user/teacher/{id}    | 取消用户教师身份 |
| 强制结束比赛 | POST   | /api/admin/contest/end/{id}     | 强制结束比赛   |

### 5.3 接口规范

#### 5.3.1 统一响应格式

所有数据API返回统一格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 字段      | 类型      | 说明                                               |
| ------- | ------- | ------------------------------------------------ |
| code    | Integer | 响应码：200=成功，400=参数错误，403=权限不足，404=资源不存在，500=服务器错误 |
| message | String  | 响应消息                                             |
| data    | Object  | 响应数据                                             |

#### 5.3.2 分页查询规范

所有列表接口支持分页参数：

| 参数   | 类型      | 默认值 | 说明      |
| ---- | ------- | --- | ------- |
| page | Integer | 0   | 页码，从0开始 |
| size | Integer | 20  | 每页大小    |

返回Spring Data `Page` 对象格式：

```json
{
  "content": [],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20
}
```

***

## 6. 安全设计

### 6.1 身份认证机制

**认证方式：** 基于HTTP Session的认证

**认证流程：**

1. 用户通过`POST /login`提交用户名密码
2. 系统通过BCrypt验证密码
3. 验证成功后，将User对象存入`session["currentUser"]`
4. Session有效期350分钟（配置在application.yml）
5. Redis作为Session存储

**初始管理员：**

- 应用启动时自动创建`administrator`账户
- 初始密码从环境变量`ADMIN_PASSWORD`读取，默认`123456`
- 建议生产环境修改默认密码

### 6.2 权限控制策略

**三级角色体系：**

| 角色    | 标识                  | 权限                       |
| ----- | ------------------- | ------------------------ |
| 普通用户  | 无Teacher记录          | 提交代码、参加比赛、管理团队、使用错题本     |
| 教师    | Teacher.privilege=0 | 管理后台、创建/编辑题目和比赛、审核内容     |
| 超级管理员 | Teacher.privilege=1 | 全部权限、系统配置、用户管理、AI功能、维护模式 |

**双重权限检查：**

1. **拦截器级**：在请求到达Controller前检查（MvcConfigurer配置）
2. **Controller级**：在Controller方法内部检查（业务逻辑权限）

**拦截器列表：**

| 拦截器                     | 保护路径                                        | 检查内容         |
| ----------------------- | ------------------------------------------- | ------------ |
| UnavailableInterceptor  | `/**`（排除`/judge/callback`）                | 维护模式/教师模式    |
| IconInterceptor         | `/favicon.ico`, `/logo`                      | 图标请求处理       |
| LoginViewInterceptor    | `/contest/**`, `/team/**`, `/problems/**`, `/user/**`, `/admin/**`, `/status/**`, `/forum/**` | 登录（页面重定向）    |
| LoginInterceptor        | `/api/problems/**`, `/api/status/**`, `/api/user/**`, `/api/team/**`, `/api/forum/**`, `/api/admin/**`, `/api/contest/**` | 登录（返回JSON错误） |
| TeacherCheckInterceptor | `/admin/**`, `/contest/create/0`, `/api/admin/**` | 教师及以上权限      |
| AdminCheckInterceptor   | `/admin/settings`, `/admin/user/**`, `/admin/tag/**`, `/api/admin/config`, `/api/admin/correctData`, `/api/admin/maintain`, `/api/admin/user/**`, `/api/admin/tag/**`, `/ai/**`, `/api/ai/**` | 管理员权限        |
| ContestInterceptor      | `/contest/*/**`, `/api/contest/*/**`（排除列表/入口/后台/克隆） | 比赛访问权限       |
| TeamInterceptor         | `/team/manage/**`, `/contest/create/*`, `/api/team/**`（排除列表/详情/申请/邀请） | 团队管理权限       |

> **注意**：`AIRateLimitInterceptor`（AI接口限流，每用户每分钟10次）已实现但未在MvcConfigurer中注册，当前未生效。

### 6.3 数据加密方案

| 数据类型       | 加密方式   | 说明                              |
| ---------- | ------ | ------------------------------- |
| 用户密码       | BCrypt | Spring Security提供，不可逆，强度10      |
| AI API Key | 配置文件加密 | 存储在application.yml，建议使用Jasypt加密 |

### 6.4 安全防护措施

#### 6.4.1 密码安全

- BCrypt加密存储，不可逆
- 密码长度要求：6-35字符
- 管理员初始密码从环境变量读取

#### 6.4.2 代码提交安全

- 10秒内不允许重复提交
- 代码长度限制：2\~20000字符
- 代码格式检查：必须包含主程序入口
- 判题运行在seccomp沙箱中，限制系统调用
- Python判题服务使用privileged模式，但限制内存/CPU

#### 6.4.3 AI API密钥安全

- API Key存储在application.yml，不暴露给前端
- 日志中不打印API Key和完整响应体（使用log.debug）
- AI功能仅管理员可用，有每日配额限制（100次/天）

#### 6.4.4 XSS防护

- Thymeleaf默认转义HTML
- Markdown渲染使用marked.js，配置sanitize选项
- 用户输入的代码通过`<pre>`标签原样显示
- URL参数严格验证

#### 6.4.5 维护模式

- `GlobalStatus.maintaining=true`：拦截所有请求（除判题回调）
- `GlobalStatus.teacherOnly=true`：仅允许教师及以上角色访问
- 可通过管理后台API动态切换

***

## 7. 部署设计

### 7.1 部署架构

系统采用**Docker Compose**部署，包含3个服务：

```
┌───────────────────────────────────────────────────────────┐
│                      用户浏览器                             │
└───────────────────────────────────┬───────────────────────┘
                                    │ :8080
┌───────────────────────────────────▼───────────────────────┐
│              Spring Boot 主应用 (Container)                 │
└───────────────────────────────────┬───────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
┌───────▼─────────┐       ┌─────────▼─────────┐     ┌───────▼─────────┐
│   MySQL 8.0    │       │   Python Judger  │     │   Redis         │
│   (Container)  │       │   (Container)     │     │   (Container)   │
│   :13306       │       │   :12345         │     │   :16379        │
└─────────────────┘       └───────────────────┘     └─────────────────┘
```

**Docker Compose配置：**

```yaml
services:
  db:
    image: mysql:8.0.19
    ports:
      - "13306:3306"
    volumes:
      - "./data/mysql:/var/lib/mysql"
    environment:
      - MYSQL_ROOT_PASSWORD=123456
      - MYSQL_DATABASE=oj
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:latest
    ports:
      - "16379:6379"

  judger:
    build: ./judger_source
    privileged: true
    ports:
      - "12345:8080"
    volumes:
      - "./data/ojdata:/ojdata:ro"
    environment:
      - OJ_BACKEND_CALLBACK=http://host.docker.internal:8080/judge/callback
```

### 7.2 环境配置要求

#### 7.2.1 系统环境

| 环境             | 要求                     |
| -------------- | ---------------------- |
| 操作系统           | Linux（推荐）/ Windows 10+ |
| Docker         | 20.10+                 |
| Docker Compose | 2.0+                   |
| 内存             | 4GB+（推荐8GB）            |
| 磁盘             | 20GB+                  |

#### 7.2.2 配置文件

**application.yml主要配置项：**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:13306/oj?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
  redis:
    host: localhost
    port: 16379

oj:
  url: http://localhost:8080
  judger-host: ["http://localhost:12345"]
  test-data-path: "./data/ojdata"

ai:
  api:
    url: https://aicanapi.com/v1
    key: sk-...
  model:
    default: gpt-4o-mini
  max-tokens: 4096
  temperature: 0.7
  quota:
    admin-daily: 100
```

### 7.3 资源需求估算

| 服务             | CPU    | 内存        | 磁盘            |
| -------------- | ------ | --------- | ------------- |
| Spring Boot主应用 | 2核     | 2GB       | 100MB         |
| MySQL 8.0      | 1核     | 1GB       | 10GB+（取决于数据量） |
| Redis          | 1核     | 512MB     | 100MB         |
| Python Judger  | 2核     | 1GB       | 100MB         |
| **总计**         | **6核** | **4.5GB** | **10GB+**     |

**测试数据存储：**

- 每道题测试数据平均：10MB
- 100道题：1GB
- 建议预留10GB+磁盘空间

***

## 8. 性能设计

### 8.1 性能指标要求

| 指标       | 要求          |
| -------- | ----------- |
| 首页响应时间   | < 500ms     |
| 题目列表响应时间 | < 1000ms    |
| 提交代码响应时间 | < 500ms（异步） |
| 判题完成时间   | < 60s（平均）   |
| 并发用户数    | 100+        |
| TPS      | 100+        |

### 8.2 性能瓶颈分析

| 瓶颈        | 原因                     |
| --------- | ---------------------- |
| 数据库查询     | 未加索引的查询，关联查询过多         |
| 判题服务      | 测试用例多时，单台判题机处理能力有限     |
| 缓存未命中     | 热点数据未缓存或缓存过期           |
| Session管理 | Session存储在内存中，分布式部署有问题 |

### 8.3 性能优化策略

#### 8.3.1 缓存策略

- **Redis缓存**：Spring Cache注解，TTL 120秒
- **缓存数据**：题目列表、比赛排名、用户统计、标签列表
- **Session存储**：Redis存储Session，支持分布式部署

#### 8.3.2 数据库优化

- **HikariCP连接池**：最大连接数10，性能优秀
- **索引优化**：在username、problem\_id、user\_id等字段加索引
- **JPA懒加载**：Contest、Team等关联实体使用懒加载
- **分页查询**：避免全表扫描，使用分页

#### 8.3.3 判题性能

- **多进程判题池**：CPU核心数+1个进程
- **异步回调**：不阻塞主线程
- **负载均衡**：支持多台判题机，轮询分配
- **超时保护**：5分钟自动标记System Error

#### 8.3.4 并发控制

- **Tomcat最大线程数**：2000
- **AI工作流内存Map**：30分钟自动清理过期数据
- **提交频率限制**：10秒冷却期

### 8.4 性能测试方法

| 测试类型   | 测试工具   | 测试目标            |
| ------ | ------ | --------------- |
| 接口性能测试 | JMeter | 验证响应时间、TPS、并发能力 |
| 压力测试   | JMeter | 验证系统在高负载下的稳定性   |
| 判题性能测试 | 自定义脚本  | 验证批量提交判题的处理时间   |

***

## 9. 扩展性设计

### 9.1 扩展策略

#### 9.1.1 水平扩展判题服务

- 支持多台判题机同时工作，通过`config.judgerhost`列表配置
- 负载均衡策略：轮询（Round Robin）
- 每台判题机独立部署，互不影响
- 判题结果通过HTTP回调返回主应用

#### 9.1.2 垂直扩展主应用

- 增加JVM内存：`-Xmx4g`
- 增加CPU核心数
- 增加HikariCP连接池大小

#### 9.1.3 数据库扩展

- 读写分离：主库负责写入，从库负责读取
- 分库分表：按用户ID或题目ID分库

### 9.2 模块解耦设计

#### 9.2.1 主应用与判题机解耦

- 通信方式：HTTP REST API
- 协议：JSON
- 无状态：判题机不保存状态，所有状态在主应用
- 可替换：可替换为其他判题服务实现

#### 9.2.2 AI服务解耦

- 支持多种API格式（/responses、/chat/completions、/completions）
- 自动fallback机制
- 可替换为其他AI模型API
- 配置化模型选择

***

## 10. 测试策略

### 10.1 测试类型

| 测试类型  | 说明                        | 测试目标         |
| ----- | ------------------------- | ------------ |
| 单元测试  | 对Service、Repository进行单元测试 | 验证核心业务逻辑的正确性 |
| 集成测试  | 测试Controller、拦截器、数据库集成    | 验证端到端功能的正确性  |
| 接口测试  | 测试API接口的功能与性能             | 验证接口的正确性与性能  |
| 安全性测试 | 测试XSS、SQL注入、权限绕过          | 验证系统的安全性     |
| 性能测试  | 测试响应时间、并发能力、TPS           | 验证系统的性能指标    |

### 10.2 测试工具

| 工具        | 用途        |
| --------- | --------- |
| JUnit 5   | 单元测试框架    |
| Mockito   | Mock框架    |
| JMeter    | 性能测试与压力测试 |
| Postman   | 接口测试      |
| OWASP ZAP | 安全性测试     |

### 10.3 测试用例示例

#### 10.3.1 核心业务测试用例

| 测试用例       | 操作                  | 预期结果                |
| ---------- | ------------------- | ------------------- |
| 用户登录成功     | 提交正确的用户名密码          | 返回200，Session中有用户   |
| 用户登录失败     | 提交错误的密码             | 返回403               |
| 提交代码AC     | 提交正确的代码             | 返回PENDING，等待后状态变为AC |
| 提交代码WA     | 提交错误的代码             | 返回PENDING，等待后状态变为WA |
| 管理员审核题目    | 管理员审核通过题目           | 题目状态变为APPROVED      |
| AI辅助出题     | 管理员发起AI出题请求         | 4步流程成功，题目正式发布       |
| 普通用户访问AI页面 | 普通用户访问/ai/assistant | 返回403，被拦截           |

#### 10.3.2 安全性测试用例

| 测试用例      | 操作            | 预期结果             |
| --------- | ------------- | ---------------- |
| SQL注入     | 在搜索框输入SQL注入语句 | 返回400，或查询结果正常无注入 |
| XSS攻击     | 在评论输入XSS脚本    | 脚本被转义，不执行        |
| 权限绕过      | 未登录访问管理后台     | 返回403，被拦截        |
| 普通用户修改管理员 | 普通用户调用设置教师接口  | 返回403，被拦截        |

