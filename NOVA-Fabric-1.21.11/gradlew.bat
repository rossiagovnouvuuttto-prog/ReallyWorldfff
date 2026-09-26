@echo off
setlocal
set "APP_HOME=%~dp0"
if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java.exe"
)
"%JAVA_EXE%" -version >nul 2>&1
if errorlevel 1 (
  echo ERROR: Java 21 is required and was not found. Set JAVA_HOME or add java to PATH. 1>&2
  exit /b 1
)
"%JAVA_EXE%" -jar "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" %*
exit /b %ERRORLEVEL%
