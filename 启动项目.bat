@echo off
chcp 65001 >nul
title xyy-login-vue 一键启动

echo ==============================================
echo   xyy-login-vue 一键启动
echo   后端: Spring Boot (8081)   前端: Vite (5173)
echo ==============================================
echo.

echo [1/2] 启动后端 Spring Boot (8081)...
start "backend-8081" cmd /k "cd /d D:\workSpace\springBootTest && mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8081"

echo [2/2] 启动前端 Vite (5173)...
start "frontend-5173" cmd /k "cd /d C:\Users\Administrator\Desktop\xyy123-main\xyy123-main && npm run dev"

echo.
echo 浏览器将在 8 秒后自动打开 http://localhost:5173
echo （首次启动后端要下载依赖，多等一会儿；两个窗口都别关）
timeout /t 8 >nul
start http://localhost:5173
