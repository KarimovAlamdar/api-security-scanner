@echo off
if not defined JAVA_HOME (
    if exist "E:\Java\jdk-25.0.2" set "JAVA_HOME=E:\Java\jdk-25.0.2"
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0"
java demo\VulnerableDemoServer.java
pause
