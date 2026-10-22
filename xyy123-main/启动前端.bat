@echo off
chcp 936 >nul
title frontend-5173 前端启动

setlocal
set "FRONTEND_DIR=%~dp0"

echo ==============================================
echo   前端启动：Vue3 + Vite (5173)
echo   目录：%FRONTEND_DIR%
echo ==============================================
echo.

:: 1. 前端依赖（缺失则安装，走国内镜像）
if not exist "%FRONTEND_DIR%node_modules" (
  echo [1/2] 首次运行，安装前端依赖 ...
  cd /d "%FRONTEND_DIR%"
  call npm install --registry=https://registry.npmmirror.com
  if errorlevel 1 (
    echo [错误] 依赖安装失败，请检查网络后重试。
    pause
    exit /b 1
  )
) else (
  echo [1/2] 前端依赖已存在，跳过。
)

:: 2. 启动前端（Vite 会把 /api 代理到 8081，后端没开的话接口会报错）
netstat -ano 2>nul | findstr ":5173" | findstr "LISTENING" >nul
if errorlevel 1 (
  echo [2/2] 启动 Vite 开发服务器 ...
  echo       提示：请确保后端已启动（8081），否则登录接口连不上。
  cd /d "%FRONTEND_DIR%"
  call npm run dev
) else (
  echo [2/2] 前端 5173 已在运行，无需重复启动。
  timeout /t 3 >nul
  exit /b 0
)

echo.
echo 前端已停止（窗口关闭即退出）。
pause
