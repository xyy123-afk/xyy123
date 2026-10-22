@echo off
chcp 936 >nul
title 停止 xyy-login 项目

echo 正在停止 xyy-login 项目 (后端 8081 / 前端 5173) ...

:: 按窗口标题关闭(start 脚本设定的标题)
taskkill /fi "WINDOWTITLE eq backend-8081" /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq frontend-5173" /f >nul 2>&1

:: 兜底：按端口关闭监听进程
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":8081" ^| findstr "LISTENING"') do taskkill /pid %%a /f >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":5173" ^| findstr "LISTENING"') do taskkill /pid %%a /f >nul 2>&1

echo 已发送停止信号。若端口仍被占用，可再次运行本脚本确认。
timeout /t 3 >nul
