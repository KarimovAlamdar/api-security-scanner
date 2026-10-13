@echo off
if not defined JAVA_HOME (
    if exist "E:\Java\jdk-25.0.2" set "JAVA_HOME=E:\Java\jdk-25.0.2"
)
if exist "E:\apache-maven-3.10.0-bin\apache-maven-3.10.0\bin" (
    set "PATH=%JAVA_HOME%\bin;%PATH%;E:\apache-maven-3.10.0-bin\apache-maven-3.10.0\bin"
) else (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)
cd /d "%~dp0"
call mvn clean package
pause
