@echo off
title xyy-login-vue Launcher
rem ============================================
rem  xyy-login-vue - double click to start
rem  Starts Vite dev server and opens browser
rem ============================================

rem Make sure Node.js (system install) is on PATH
set "PATH=D:\Program Files\nodejs;%PATH%"

rem Switch to the folder where this .bat lives
cd /d "%~dp0"

rem Check node is available
where node >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Node.js not found. Please install Node.js first.
  pause
  exit /b 1
)

rem Start dev server in its own window (closing that window stops the server)
start "xyy-login-vue dev server" cmd /k "npm run dev"

echo Dev server is starting, waiting for port 5173...

rem Poll the port every second, up to 30 tries, then open browser
set /a tries=0
:waitloop
set /a tries+=1
if %tries% gtr 30 goto giveup
powershell -NoProfile -Command "try{$c=New-Object Net.Sockets.TcpClient('127.0.0.1',5173);$c.Close();exit 0}catch{exit 1}" >nul 2>nul
if errorlevel 1 (
  timeout /t 1 /nobreak >nul
  goto waitloop
)
goto opened

:giveup
echo [WARN] Port 5173 not ready after 30s, opening browser anyway...

:opened
start "" http://localhost:5173
echo.
echo Browser opened at http://localhost:5173
echo To stop the server, close the "xyy-login-vue dev server" window.
timeout /t 8 >nul
