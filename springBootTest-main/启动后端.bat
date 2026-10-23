@echo off
chcp 936 >nul
title backend-8081 后端启动

setlocal
set "BACKEND_DIR=%~dp0"

echo ==============================================
echo   后端启动：Spring Boot (8081)
echo   目录：%BACKEND_DIR%
echo ==============================================
echo.

:: 1. 确保 MySQL 已启动（后端连不上库会起不来）
echo [1/3] 检查 MySQL 服务 ...
net start MySQL80 >nul 2>&1
echo       完成（已运行则忽略提示）。

:: 2. 幂等建库建表（已存在则跳过，不清数据）
echo [2/3] 初始化数据库 xyy_login ...
set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if exist "%MYSQL_EXE%" (
  "%MYSQL_EXE%" -uroot -p123456 < "%BACKEND_DIR%docs\schema.sql" >nul 2>&1
  if errorlevel 1 (
    echo       [警告] 建库失败，请确认账号密码为 root/123456；若库已存在可忽略。
  ) else (
    echo       数据库就绪。
  )
) else (
  echo       [警告] 未找到 mysql.exe，若库已存在可忽略。
)

:: 3. 启动后端（端口必须显式指定，防端口劫持，见 AGENT.md）
netstat -ano 2>nul | findstr ":8081" | findstr "LISTENING" >nul
if errorlevel 1 (
  echo [3/3] 启动 Spring Boot ...
  cd /d "%BACKEND_DIR%"
  call mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
) else (
  echo [3/3] 后端 8081 已在运行，无需重复启动。
  timeout /t 3 >nul
  exit /b 0
)

echo.
echo 后端已停止（窗口关闭即退出）。
pause
