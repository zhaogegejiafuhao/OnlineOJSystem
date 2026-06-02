@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul 2>&1
title OJ System - Stop

echo ============================================================
echo    Online Judge System - 停止服务
echo ============================================================
echo.

cd /d "%~dp0"

echo [1/2] 停止 Docker 容器...
docker-compose down 2>nul
echo [成功] Docker 容器已停止

echo.
echo [2/2] 检查残留 Java 进程...
for /f "tokens=2" %%p in ('tasklist /fi "imagename eq java.exe" /fo list 2^>nul ^| findstr /i "PID"') do (
    wmic process where "processid=%%p and commandline like '%%oj-system%%'" get processid 2>nul | findstr /r "[0-9]" >nul 2>&1
    if !errorlevel! equ 0 (
        echo 正在停止 Java 进程 PID: %%p
        taskkill /pid %%p /f >nul 2>&1
    )
)
echo [成功] 所有服务已停止
echo.
timeout /t 3 /nobreak >nul
