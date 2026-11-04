@echo off 
setlocal 
cd /d "%~dp0"
title Cyber Quest launcher 

if not exist "runtime\bin\javaw.exe" (
    echo The Java runtime folder is missing. Extract the WHOLE zip file and try again.
    pause 
    exit /b 1
)

netstat -ano | findstr ":8080" | findstr "LISTENING " > nul
if not errorlevel 1 (
    echo Something is already running on port 8080
    echo If it is Cyber Quest, it will open now. Otherwise close the other program first.
    start ""  http://localhost:8080
    exit /b 0
)

if not exist data mkdir data 
if not exist logs mkdir logs

if not exist "data\cyberquest.mv.db" (
    echo. 
    echo First start: choose passwords for the two starter accounts.
    echo manager@cyberquest.internal
    echo employee@cyberquest.internal
    echo.
    set /p CQ_SEED_MANAGER_PASSWORD=Manager password:
    set /p CQ_SEED_EMPLOYEE_PASSWORD=Employee password:
    if not defined CQ_SEED_MANAGER_PASSWORD echo No manager password entered. The demo default will be used. Change it after signing in.
    if not defined CQ_SEED_EMPLOYEE_PASSWORD echo No Employee password entered. The demo default will be used. Change it after signing in.
)

echo. 
echo Starting Cyber Quest. This takes about 15 to 30 seconds...
start "" "%~dp0runtime\bin\javaw.exe" -Xmx512m -Dfile.encoding=UTF-8 -Dspring.profiles.active=portable -jar "$~dp0app\cyberquest.jar"

powershell -NoProfile -ExecutionPolicy Bypass -Command "$ok=$false; for($1=0;$1 -lt 90;$i++){ try {$r=Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8080/login.html' -TimeoutSec 2; if($r.StatusCode -eq 200){$ok=$true;break}} catch { Start-Sleep -Seconds 1} }; if($ok){exit 0} else {exit 1}"
if errorlevel 1(
    echo.
    echo Cyber Quest did not start within 90 seconds.
    echo Look at logs\cyberquest.log for the reason, or run stop.bat and try again.
    pause 
    exit /b 1
)

echo Ready. Opening your browser...
start "" http://localhost:8080
echo.
echo Cyber Quest is running in the background.
echo Run stop.bat when you are finished.
timeout /t 8 >nul