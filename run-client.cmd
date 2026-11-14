@echo off
setlocal
rem Keep the console decoder in sync with the UTF-8 Logback encoders.
for /f "tokens=2 delims=:" %%C in ('chcp') do set "SMS_PREVIOUS_CODEPAGE=%%C"
chcp 65001 >nul
pushd "%~dp0"
call "%~dp0mvnw.cmd" -pl client javafx:run %*
set "SMS_EXIT_CODE=%ERRORLEVEL%"
popd
if defined SMS_PREVIOUS_CODEPAGE chcp %SMS_PREVIOUS_CODEPAGE% >nul
exit /b %SMS_EXIT_CODE%
