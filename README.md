# OnlineOJ System Distribution (Source Code Edition)

## 简介 (Introduction)
这是一个开箱即用的在线判题系统（Online Judge），包含**完整前后端源代码**、判题机源码、数据库及 Redis 缓存。

## 目录结构 (Directory Structure)
- `src/`: **后端 Java Spring Boot 源代码** (含前端资源)
- `pom.xml`: Maven 构建文件
- `judger_source/`: **判题机 Python 源代码**
- `acm.jar`: 预编译的后端程序（方便快速体验）
- `docker-compose.yml`: 基础设施服务的 Docker 编排文件
- `init_data.sql`: 数据库初始化脚本（包含表结构和 11 道题目/用户的初始数据）
- `data/`: 数据存储目录
  - `ojdata/`: 题目测试数据
- `start.bat`: Windows 一键启动脚本

## 快速开始 (Quick Start)

### 方式一：快速体验 (使用预编译包)
1. 确保安装了 Docker Desktop 和 Java 8。
2. 双击运行 `start.bat`。
3. 访问：[http://localhost:8080/](http://localhost:8080/)

### 方式二：从源码运行 (使用 Maven)
1. 启动基础设施：`docker-compose up -d`
2. 进入源码目录：`cd src`
3. 运行项目：`mvn spring-boot:run` (或使用 IDEA 打开 `pom.xml` 运行)
4. 注意：源码中的 `application.yml` 已配置为连接 `localhost` 的数据库。

## 判题机源码说明 (Judger Source)
- 判题机源码位于 `judger_source/` 目录。
- 默认情况下，系统使用预构建的镜像。
- 如果您修改了判题机代码并想生效，请修改 `docker-compose.yml`：
  1. 注释掉 `image: ...` 行
  2. 取消注释 `build: ./judger_source` 行
  3. 运行 `docker-compose up -d --build`

## 默认账户 (Default Accounts)
- **管理员**: `admin` / `123456`
- **备用管理员**: `administrator` / `123456`
- **普通用户**: `user_2` 到 `user_11` / `123456`

## 常见问题 (Q&A)
- **数据库连接失败**: 请确保端口 13306 未被占用。
- **判题错误**: 确保 `data/ojdata` 中有对应的测试数据文件。
