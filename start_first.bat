@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul 2>&1
title OJ System - 首次部署初始化
color 0C

echo ============================================================
echo    Online Judge System - 首次部署初始化
echo    此脚本会删除旧数据库并重新初始化，仅首次部署时使用！
echo    日常启动请使用 start.bat
echo ============================================================
echo.

choice /C YN /M "确认首次部署？这将删除所有旧数据"
if !errorlevel! neq 2 (
    if !errorlevel! equ 2 (
        echo 已取消
        pause
        exit /b 0
    )
)

set "BASE_DIR=%~dp0"
set "BASE_DIR=%BASE_DIR:~0,-1%"
set "APP_JAR=%BASE_DIR%\target\acm-0.0.1-SNAPSHOT.jar"
set "DATA_DIR=%BASE_DIR%\data"
set "OJDATA_DIR=%DATA_DIR%\ojdata"
set "SQL_FILE=%BASE_DIR%\init_data.sql"
set "APP_PORT=8080"

echo [1/5] 检查 Java 环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Java 环境！
    echo 本系统需要 JDK 8 或更高版本，请先安装：
    echo 下载地址: https://adoptium.net/
    choice /C YN /M "是否尝试使用 winget 自动安装 JDK 8"
    if !errorlevel! equ 1 (
        echo 正在安装 JDK 8，请稍候...
        winget install EclipseAdoptium.Temurin.8.JDK --accept-package-agreements --accept-source-agreements
        if !errorlevel! neq 0 (
            echo [错误] 自动安装失败，请手动安装 JDK 8 后重新运行此脚本
            pause
            exit /b 1
        )
        echo [成功] JDK 8 安装完成！
        echo [提示] 可能需要重新打开命令行窗口以使 Java 命令生效
        echo 请关闭此窗口，重新打开后再运行 start.bat
        pause
        exit /b 0
    ) else (
        echo 安装 JDK 8 后，重新运行此脚本即可
        pause
        exit /b 1
    )
)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    echo [成功] Java 版本: %%v
)
echo.

echo [2/5] 检查 Docker 环境...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Docker！
    echo 本系统需要 Docker Desktop 来运行数据库和判题服务，请先安装：
    echo 下载地址: https://www.docker.com/products/docker-desktop/
    choice /C YN /M "是否尝试使用 winget 自动安装 Docker Desktop"
    if !errorlevel! equ 1 (
        echo 正在安装 Docker Desktop，请稍候...
        winget install Docker.DockerDesktop --accept-package-agreements --accept-source-agreements
        if !errorlevel! neq 0 (
            echo [错误] 自动安装失败，请手动安装 Docker Desktop 后重新运行此脚本
            pause
            exit /b 1
        )
        echo [成功] Docker Desktop 安装完成！
        echo [重要] 需要重启电脑并启动 Docker Desktop 后再运行此脚本
        pause
        exit /b 0
    ) else (
        echo 安装 Docker Desktop 并启动后，重新运行此脚本即可
        pause
        exit /b 1
    )
)
for /f "tokens=*" %%v in ('docker --version') do echo [成功] %%v

docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] Docker 已安装但未运行！
    echo 正在尝试启动 Docker Desktop...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe" 2>nul
    echo 等待 Docker 启动...
    set DOCKER_READY=0
    for /L %%i in (1,1,30) do (
        if !DOCKER_READY! equ 0 (
            docker info >nul 2>&1
            if !errorlevel! equ 0 (
                set DOCKER_READY=1
                echo [成功] Docker 已启动 ^(等待 %%i 次^)
            ) else (
                echo   等待 Docker 启动... %%i/30
                timeout /t 3 /nobreak >nul
            )
        )
    )
    if !DOCKER_READY! equ 0 (
        echo [错误] Docker 启动超时，请手动启动 Docker Desktop 后重试
        pause
        exit /b 1
    )
)
echo.

echo [3/5] 启动基础服务 (MySQL + Redis + Judge)...
cd /d "%BASE_DIR%"

if not exist "%OJDATA_DIR%" mkdir "%OJDATA_DIR%"
if not exist "%DATA_DIR%\judger_log" mkdir "%DATA_DIR%\judger_log"

echo [提示] 清理旧容器...
docker-compose down --remove-orphans 2>nul

echo [提示] 启动容器...
docker-compose up -d --build --remove-orphans
if %errorlevel% neq 0 (
    echo [错误] Docker 容器启动失败！
    echo 请检查 Docker Desktop 是否正在运行，端口 13306/16379/12345 是否被占用
    pause
    exit /b 1
)
echo [成功] 基础服务容器已启动
echo.

echo [4/5] 等待 MySQL 就绪...
set MYSQL_READY=0
for /L %%i in (1,1,30) do (
    if !MYSQL_READY! equ 0 (
        docker exec oj-mysql mysqladmin ping -h localhost -u root -p1234 --silent >nul 2>&1
        if !errorlevel! equ 0 (
            set MYSQL_READY=1
            echo [成功] MySQL 已就绪 ^(等待 %%i 次^)
        ) else (
            echo   等待 MySQL 启动... %%i/30
            timeout /t 2 /nobreak >nul
        )
    )
)
if %MYSQL_READY% equ 0 (
    echo [警告] MySQL 等待超时，继续尝试启动应用...
)
echo.

echo [首次部署] 删除旧数据库并重新初始化...
docker exec oj-mysql mysql -u root -p1234 -e "DROP DATABASE IF EXISTS oj; CREATE DATABASE oj DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
type "%SQL_FILE%" | docker exec -i oj-mysql mysql -u root -p1234 --default-character-set=utf8mb4 oj
if !errorlevel! equ 0 (
    echo [成功] 数据库初始化完成
) else (
    echo [警告] 数据库初始化失败，请手动执行 sql\init_data.sql
)
echo.

echo [5/5] 启动 OJ 系统...
if not exist "%APP_JAR%" (
    echo [提示] 未找到预构建 JAR，正在使用 Maven 构建...
    if exist "mvnw.cmd" (
        call mvnw.cmd clean package -DskipTests -q
    ) else (
        call mvn clean package -DskipTests -q
    )
    if !errorlevel! neq 0 (
        echo [错误] Maven 构建失败，请检查 Maven 是否已安装
        pause
        exit /b 1
    )
    echo [成功] 项目构建完成
)

echo ============================================================
echo    OJ 系统正在启动...
echo    访问地址: http://localhost:%APP_PORT%
echo    管理员账号: admin  密码: 123456
echo    按 Ctrl+C 停止服务
echo    以后请使用 start.bat 日常启动（不会删除数据）
echo ============================================================
echo.

set "TEST_DATA_PATH=%OJDATA_DIR%"
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar "%APP_JAR%" --spring.datasource.url="jdbc:mysql://localhost:13306/oj?serverTimezone=GMT%%2B8&useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci" --spring.redis.host=localhost --spring.redis.port=16379

echo.
echo [信息] OJ 系统已停止
pause
