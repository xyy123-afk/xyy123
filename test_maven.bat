@echo off
set "BACKEND_DIR=D:\workSpace\xyy123\springBootTest-main"
set "MAVEN_CMD="
if exist "%BACKEND_DIR%\mvnw.cmd" (
  set "MAVEN_CMD=%BACKEND_DIR%\mvnw.cmd"
  echo USE_WRAPPER=%MAVEN_CMD%
) else (
  for /d %%I in ("D:\IntelliJ IDEA *" "C:\Program Files\JetBrains\IntelliJ IDEA *" "C:\Program Files (x86)\JetBrains\IntelliJ IDEA *") do (
    if exist "%%I\plugins\maven\lib\maven3\bin\mvn.cmd" set "MAVEN_CMD=%%I\plugins\maven\lib\maven3\bin\mvn.cmd"
  )
  if not defined MAVEN_CMD ( where mvn >nul 2>&1 && set "MAVEN_CMD=mvn" )
)
echo RESULT_MAVEN_CMD=%MAVEN_CMD%
