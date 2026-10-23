@echo off
chcp 936 >nul
title xyy123 一键停止
setlocal EnableDelayedExpansion

REM ==========================================================
REM   停止 xyy123 / 枫林小筑 前后端服务
REM   做法：先关窗口标题，再按端口兜底。
REM   注意：绝不批量杀 java.exe —— IDEA 自身也是 Java 进程。
REM ==========================================================

set "PORTS=8081 5173"
set "BACKEND_TITLE=XYY-BACKEND-8081"
set "FRONTEND_TITLE=XYY-FRONTEND-5173"

echo ==============================================
echo   正在停止 xyy123 / 枫林小筑 服务
echo   端口：%PORTS%
echo ==============================================
echo.

REM 1. 关闭启动脚本打开的控制台窗口
echo [1/3] 关闭服务窗口 ...
taskkill /F /FI "WINDOWTITLE eq %BACKEND_TITLE%" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq %FRONTEND_TITLE%" >nul 2>&1
echo       已发送关闭指令

REM 2. 按端口兜底清理残留监听
echo [2/3] 清理端口占用 ...
for %%P in (%PORTS%) do (
  netstat -ano 2>nul | findstr ":%%P " | findstr "LISTENING" >nul
  if not errorlevel 1 (
    for /f "tokens=5" %%A in ('netstat -ano 2^>nul ^| findstr ":%%P " ^| findstr "LISTENING"') do (
      taskkill /F /PID %%A >nul 2>&1
    )
    echo       端口 %%P 已释放
  ) else (
    echo       端口 %%P 当前无占用
  )
)

REM 3. 复查
ping -n 3 127.0.0.1 >nul
echo [3/3] 复查端口 ...
set "STILL_UP=0"
for %%P in (%PORTS%) do (
  netstat -ano 2>nul | findstr ":%%P " | findstr "LISTENING" >nul
  if not errorlevel 1 (
    echo       [占用] 端口 %%P 仍有进程监听
    for /f "tokens=5" %%B in ('netstat -ano 2^>nul ^| findstr ":%%P " ^| findstr "LISTENING"') do echo              PID=%%B
    set "STILL_UP=1"
  )
)

echo.
echo ==============================================
if "!STILL_UP!"=="1" (
  echo   仍有端口被占用，通常是权限不足。
  echo   请以管理员身份重新运行本脚本。
) else (
  echo   全部服务已停止，端口已释放。
)
echo ==============================================
echo.
pause
