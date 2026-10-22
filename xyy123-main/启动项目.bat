@echo off
chcp 936 >nul
title 枫林小筑 一键启动

setlocal
set "FRONTEND_DIR=%~dp0"
set "BACKEND_DIR=%~dp0..\springBootTest-main"
:: 兼容布局：后端若与前端同在「新建文件夹」等同一目录，回退到同级查找
if not exist "%BACKEND_DIR%\pom.xml" set "BACKEND_DIR=%~dp0springBootTest-main"
set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

echo ==============================================
echo   枫林小筑（博客）前后端一键启动
echo   后端 Spring Boot (8081)   前端 Vite (5173)
echo ==============================================
echo.

:: 0. 源码兜底：若项目目录丢失，尝试从 .rar 解压
if not exist "%BACKEND_DIR%\pom.xml" (
  echo [准备] 未发现后端源码，尝试解压 .rar ...
  call :extract springBootTest-main
)
if not exist "%FRONTEND_DIR%\package.json" (
  echo [准备] 未发现前端源码，尝试解压 .rar ...
  call :extract xyy123-main
)

if not exist "%BACKEND_DIR%\pom.xml" (
  echo [错误] 仍找不到后端源码(pom.xml)。请先把 springBootTest-main 解压到本脚本同级目录。
  goto :fail
)
if not exist "%FRONTEND_DIR%\package.json" (
  echo [错误] 仍找不到前端源码(package.json)。请先把 xyy123-main 解压到本脚本同级目录。
  goto :fail
)

:: 1. 确保 MySQL 服务已启动
echo [1/5] 确保 MySQL 服务已启动 ...
net start MySQL80 >nul 2>&1
echo       完成(若已运行会提示，可忽略)。

:: 2. 建库建表(幂等，不会清数据)
echo [2/5] 初始化数据库 xyy_login (已存在则跳过) ...
if exist "%MYSQL_EXE%" (
  "%MYSQL_EXE%" -uroot -p123456 --default-character-set=utf8mb4 < "%BACKEND_DIR%\docs\schema.sql" >nul 2>&1
  if errorlevel 1 (
    echo       [警告] 建库脚本执行失败，请检查 MySQL 账号密码是否为 root/123456。
  ) else (
    echo       数据库就绪。
  )
) else (
  echo       [警告] 未找到 mysql.exe，请确认 MySQL 已安装；若库已存在可忽略。
)

:: 3. 前端依赖(缺失则安装)
if not exist "%FRONTEND_DIR%node_modules" (
  echo [3/5] 首次运行，安装前端依赖 ...
  pushd "%FRONTEND_DIR%"
  call npm install --registry=https://registry.npmmirror.com
  popd
) else (
  echo [3/5] 前端依赖已存在，跳过。
)

:: 4. 启动后端(端口必须显式指定，防端口劫持)
netstat -ano 2>nul | findstr ":8081" | findstr "LISTENING" >nul
if errorlevel 1 (
  echo [4/5] 启动后端 Spring Boot (8081) ...
  start "backend-8081" cmd /k cd /d "%BACKEND_DIR%" ^&^& mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
) else (
  echo [4/5] 后端 8081 已在运行，跳过。
)

:: 5. 启动前端
netstat -ano 2>nul | findstr ":5173" | findstr "LISTENING" >nul
if errorlevel 1 (
  echo [5/5] 启动前端 Vite (5173) ...
  start "frontend-5173" cmd /k cd /d "%FRONTEND_DIR%" ^&^& npm run dev
) else (
  echo [5/5] 前端 5173 已在运行，跳过。
)

echo.
echo 启动完成！浏览器将打开 http://localhost:5173
echo 首启后端会下载 Maven 依赖，请耐心等待；两个黑窗口不要关闭。
start http://localhost:5173 >nul 2>&1
exit /b 0

:extract
set "NAME=%~1"
set "RAR=C:\Program Files\WinRAR\UnRAR.exe"
if not exist "%RAR%" goto :extract_done
for %%F in ("%FRONTEND_DIR%..\%NAME%.rar" "%FRONTEND_DIR%%NAME%.rar" "%~dp0%NAME%.rar") do (
  if exist "%%F" (
    echo   解压 %%F ...
    "%RAR%" x -o+ "%%F" "%FRONTEND_DIR%..\" >nul 2>&1
  )
)
:extract_done
goto :eof

:fail
echo.
echo 启动中止，请按上面提示准备好源码后再运行本脚本。
pause
exit /b 1
