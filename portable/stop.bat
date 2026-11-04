@echo off 
setlocal 
set found=0 
for /f "token=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING" ') do (
    taskkill /PID %%a /F >nul 2>&1
    set found=1
)
if "%found%"=="1"(
    echo Cyber Quest has been stopped.
) else (
    echo Cyber Quest was not running.
)
timeout /t 4>nul 