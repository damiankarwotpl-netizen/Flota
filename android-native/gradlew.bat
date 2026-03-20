@echo off
setlocal
set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%
if exist "%USERPROFILE%\.local\share\mise\installs\java\17.0.2\bin\java.exe" (
  set JAVA_HOME=%USERPROFILE%\.local\share\mise\installs\java\17.0.2
  set PATH=%JAVA_HOME%\bin;%PATH%
)
gradle -p "%PROJECT_DIR%" %*
