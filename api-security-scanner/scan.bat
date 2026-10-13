@echo off
if not defined JAVA_HOME (
    if exist "E:\Java\jdk-25.0.2" set "JAVA_HOME=E:\Java\jdk-25.0.2"
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0"
java -jar target\api-security-scanner.jar --spec openapi.json --base-url http://127.0.0.1:8080 --user-a-token tokenUserA --user-b-token tokenUserB --scan-source demo --sarif-output scan-report.sarif
echo.
pause
