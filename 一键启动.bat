@echo off
chcp 936 >nul
title xyy123 一键启动
setlocal EnableDelayedExpansion

REM ==========================================================
REM   xyy123 / 枫林小筑 前后端一键启动
REM     后端 Spring Boot : 8081（端口固化，防环境变量劫持）
REM     前端 Vue3 + Vite : 5173
REM   首次运行会下载 Maven / npm 依赖，耗时长属正常
REM ==========================================================

REM ---------- 可调参数 ----------
set "BACKEND_PORT=8081"
set "FRONTEND_PORT=5173"
set "NPM_REGISTRY=https://registry.npmmirror.com"
set "DB_PASSWORD=123456"
set "BACKEND_TITLE=XYY-BACKEND-8081"
set "FRONTEND_TITLE=XYY-FRONTEND-5173"
set "WAIT_LIMIT=240"

echo ==============================================
echo   xyy123 / 枫林小筑 一键启动
echo   后端 Spring Boot ^(%BACKEND_PORT%^)   前端 Vite ^(%FRONTEND_PORT%^)
echo ==============================================
echo.

REM ---------- 1/7 定位项目目录（自动探测常见摆放方式） ----------
echo [1/7] 定位项目目录 ...

set "FRONTEND_DIR="
for %%D in ("%~dp0xyy123-main" "%~dp0") do (
  if not defined FRONTEND_DIR if exist "%%~D\package.json" set "FRONTEND_DIR=%%~D"
)

set "BACKEND_DIR="
for %%D in ("%~dp0springBootTest-main" "%~dp0springBootTest" "%~dp0..\springBootTest" "%~dp0..\springBootTest-main" "D:\workSpace\springBootTest") do (
  if not defined BACKEND_DIR if exist "%%~D\pom.xml" set "BACKEND_DIR=%%~D"
)

echo       前端目录：!FRONTEND_DIR!
echo       后端目录：!BACKEND_DIR!
echo.

if not defined FRONTEND_DIR (
  echo [错误] 找不到前端目录 package.json，无法启动。
  goto :fail
)
if not defined BACKEND_DIR (
  echo [提示] 未找到后端 ^(pom.xml^)，将只以纯前端模式启动。
  set "SKIP_BACKEND=1"
)

REM ---------- 2/7 运行环境自检 ----------
echo [2/7] 检查运行环境 ...

java -version >nul 2>&1
if errorlevel 1 (
  echo       [错误] 未检测到 Java，请先安装 JDK 17 及以上。
  goto :fail
) else (
  for /f "tokens=2,3 delims= " %%J in ('java -version 2^>^&1 ^| findstr /i "version"') do echo       Java 已就绪：%%~J
)

node -v >nul 2>&1
if errorlevel 1 (
  echo       [错误] 未检测到 Node.js，请先安装 Node 18 及以上。
  goto :fail
) else (
  for /f "tokens=1" %%N in ('node -v') do echo       Node 已就绪：%%N
)

if not defined SKIP_BACKEND (
  set "MAVEN_CMD="
  if exist "D:\IntelliJ IDEA 2024.3.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" (
    set "MAVEN_CMD=D:\IntelliJ IDEA 2024.3.2.2\plugins\maven\lib\maven3\bin\mvn.cmd"
  )
  if not defined MAVEN_CMD (
    for /d %%I in ("D:\IntelliJ IDEA *" "C:\Program Files\JetBrains\IntelliJ IDEA *") do (
      if exist "%%~I\plugins\maven\lib\maven3\bin\mvn.cmd" set "MAVEN_CMD=%%~I\plugins\maven\lib\maven3\bin\mvn.cmd"
    )
  )
  if not defined MAVEN_CMD if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
  if not defined MAVEN_CMD if exist "!BACKEND_DIR!\mvnw.cmd" set "MAVEN_CMD=!BACKEND_DIR!\mvnw.cmd"
  if not defined MAVEN_CMD (
    for /f "delims=" %%M in ('where mvn 2^>nul') do if not defined MAVEN_CMD set "MAVEN_CMD=%%M"
  )
  if not defined MAVEN_CMD (
    echo       [错误] 未找到 Maven，且项目中没有 mvnw.cmd。
    goto :fail
  )
  echo       Maven 就绪：!MAVEN_CMD!
)

REM ---------- 3/7 MySQL 检查（可选，失败不阻断） ----------
echo [3/7] 检查 MySQL ...
netstat -ano 2>nul | findstr ":3306 " | findstr "LISTENING" >nul
if not errorlevel 1 (
  echo       MySQL 已在运行 ^(3306^)
) else (
  for %%S in (MySQL80 MySQL84 MySQL57 MySQL) do net start %%S >nul 2>&1
  netstat -ano 2>nul | findstr ":3306 " | findstr "LISTENING" >nul
  if not errorlevel 1 (
    echo       MySQL 已启动 ^(3306^)
  ) else (
    echo       [提示] MySQL 未运行 ^(或需管理员权限启动^)。
  )
)

REM ---------- 4/7 数据库初始化（docs/schema.sql 幂等，可重复执行） ----------
echo [4/7] 初始化数据库 xyy_login ...
if defined SKIP_BACKEND goto :skipDb
if not exist "!BACKEND_DIR!\docs\schema.sql" (
  echo       [提示] 未找到 docs\schema.sql，跳过建库。
  goto :skipDb
)
set "MYSQL_EXE="
for %%P in ("C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" "C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe") do (
  if not defined MYSQL_EXE if exist "%%~P" set "MYSQL_EXE=%%~P"
)
if not defined MYSQL_EXE (
  echo       [提示] 未找到 mysql.exe，跳过建库，请确保库已存在。
  goto :skipDb
)
"!MYSQL_EXE!" -uroot -p!DB_PASSWORD! --default-character-set=utf8mb4 < "!BACKEND_DIR!\docs\schema.sql" >nul 2>&1
if errorlevel 1 (
  echo       [警告] 建库失败，常见原因：密码不是 !DB_PASSWORD! 或 MySQL 未就绪。
  echo              可修改本脚本顶部的 DB_PASSWORD 后重试。继续启动。
) else (
  echo       建库/校验完成 ^(幂等，可重复执行^)
)
:skipDb

REM ---------- 5/7 释放端口 ----------
echo [5/7] 清理端口 %BACKEND_PORT% / %FRONTEND_PORT% ...
call :freePort %BACKEND_PORT%
call :freePort %FRONTEND_PORT%

REM ---------- 6/7 前端依赖 ----------
echo [6/7] 检查前端依赖 ...
if not exist "!FRONTEND_DIR!\node_modules" (
  echo       首次安装，请耐心等待 ^(通常 1-3 分钟^) ...
  pushd "!FRONTEND_DIR!"
  call npm install --registry=!NPM_REGISTRY!
  if errorlevel 1 (
    popd
    echo       [错误] npm install 失败，请查看上方报错信息。
    goto :fail
  )
  popd
  echo       依赖安装完成。
) else (
  echo       node_modules 已存在，跳过安装。
)

REM ---------- 7/7 启动服务 ----------
echo [7/7] 启动服务 ...
if not defined SKIP_BACKEND (
  start "!BACKEND_TITLE!" cmd /k cd /d "!BACKEND_DIR!" ^&^& call "!MAVEN_CMD!" spring-boot:run -Dspring-boot.run.arguments=--server.port=!BACKEND_PORT!
  echo       后端窗口已打开：!BACKEND_TITLE!
)

start "!FRONTEND_TITLE!" cmd /k cd /d "!FRONTEND_DIR!" ^&^& npm run dev
echo       前端窗口已打开：!FRONTEND_TITLE!

echo.
echo 正在等待服务就绪 ...
if not defined SKIP_BACKEND call :waitPort %BACKEND_PORT% 后端
call :waitPort %FRONTEND_PORT% 前端

echo.
echo ==============================================
echo   启动流程结束
echo     前端首页：http://localhost:%FRONTEND_PORT%
if not defined SKIP_BACKEND echo     后端接口：http://localhost:%BACKEND_PORT%
echo     站长账号：demo001 / 123456
echo   日常关闭请运行：一键停止.bat
echo ==============================================
start "" http://localhost:%FRONTEND_PORT% >nul 2>&1
echo.
pause
exit /b 0

REM ---------- 子程序：释放指定端口 ----------
:freePort
set "PORT=%~1"
netstat -ano 2>nul | findstr ":%PORT% " | findstr "LISTENING" >nul
if errorlevel 1 (
  echo       端口 %PORT% 空闲
  goto :eof
)
for /f "tokens=5" %%P in ('netstat -ano 2^>nul ^| findstr ":%PORT% " ^| findstr "LISTENING"') do (
  taskkill /F /PID %%P >nul 2>&1
)
ping -n 3 127.0.0.1 >nul
echo       端口 %PORT% 已释放
goto :eof

REM ---------- 子程序：等待端口进入 LISTENING ----------
:waitPort
set "PORT=%~1"
set "NAME=%~2"
set /a SEC=0
:waitLoop
ping -n 4 127.0.0.1 >nul
netstat -ano 2>nul | findstr ":%PORT% " | findstr "LISTENING" >nul
if not errorlevel 1 (
  echo       !NAME! ^(%PORT%^) 已就绪，耗时约 !SEC! 秒
  goto :eof
)
set /a SEC+=3
if !SEC! GEQ %WAIT_LIMIT% (
  echo       [超时] !NAME! ^(%PORT%^) 未在 %WAIT_LIMIT% 秒内就绪。
  echo              首次运行需下载依赖，请到对应窗口观察日志，就绪后刷新浏览器即可。
  goto :eof
)
echo       !NAME! ^(%PORT%^) 启动中 ... 已等待 !SEC! 秒
goto :waitLoop

:fail
echo.
echo 启动未完成，请按上方提示处理后重试。
pause
exit /b 1
