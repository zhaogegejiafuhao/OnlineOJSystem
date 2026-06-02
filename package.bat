@echo off
chcp 65001 >nul 2>&1
title OJ System - Package Builder

echo ============================================================
echo    Online Judge System - 项目打包
echo ============================================================
echo.

set "PROJECT_DIR=%~dp0"
set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"
set "OUTPUT_DIR=%PROJECT_DIR%\dist"
set "PACKAGE_NAME=OJSystem-Deploy"

echo [1/3] 构建 JAR 包...
cd /d "%PROJECT_DIR%"
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo [错误] Maven 构建失败
    pause
    exit /b 1
)
echo [成功] JAR 包构建完成
echo.

echo [2/3] 准备打包目录...
if exist "%OUTPUT_DIR%\%PACKAGE_NAME%" rmdir /s /q "%OUTPUT_DIR%\%PACKAGE_NAME%"
mkdir "%OUTPUT_DIR%\%PACKAGE_NAME%"

mkdir "%OUTPUT_DIR%\%PACKAGE_NAME%\target"
copy "%PROJECT_DIR%\target\acm-0.0.1-SNAPSHOT.jar" "%OUTPUT_DIR%\%PACKAGE_NAME%\target\" >nul

mkdir "%OUTPUT_DIR%\%PACKAGE_NAME%\judger_source"
xcopy "%PROJECT_DIR%\judger_source" "%OUTPUT_DIR%\%PACKAGE_NAME%\judger_source\" /E /I /Q >nul

copy "%PROJECT_DIR%\docker-compose.yml" "%OUTPUT_DIR%\%PACKAGE_NAME%\" >nul
copy "%PROJECT_DIR%\start.bat" "%OUTPUT_DIR%\%PACKAGE_NAME%\" >nul
copy "%PROJECT_DIR%\stop.bat" "%OUTPUT_DIR%\%PACKAGE_NAME%\" >nul
copy "%PROJECT_DIR%\pom.xml" "%OUTPUT_DIR%\%PACKAGE_NAME%\" >nul

mkdir "%OUTPUT_DIR%\%PACKAGE_NAME%\src"
xcopy "%PROJECT_DIR%\src" "%OUTPUT_DIR%\%PACKAGE_NAME%\src\" /E /I /Q >nul

mkdir "%OUTPUT_DIR%\%PACKAGE_NAME%\data\ojdata"
mkdir "%OUTPUT_DIR%\%PACKAGE_NAME%\data\judger_log"

echo [成功] 打包目录准备完成
echo.

echo [3/3] 压缩打包...
cd /d "%OUTPUT_DIR%"
powershell -Command "Compress-Archive -Path '%PACKAGE_NAME%' -DestinationPath '%PACKAGE_NAME%.zip' -Force"
if %errorlevel% neq 0 (
    echo [错误] 压缩失败
    pause
    exit /b 1
)
echo [成功] 压缩包已创建: %OUTPUT_DIR%\%PACKAGE_NAME%.zip
echo.

echo ============================================================
echo    打包完成！
echo    输出路径: %OUTPUT_DIR%\%PACKAGE_NAME%.zip
echo    
echo    部署步骤:
echo    1. 将压缩包复制到目标电脑
echo    2. 解压到任意目录
echo    3. 确保 Java 8+ 和 Docker Desktop 已安装
echo    4. 双击 start.bat 启动系统
echo    5. 访问 http://localhost:8080
echo ============================================================
echo.
pause
