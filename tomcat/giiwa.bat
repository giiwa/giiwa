@echo off
echo "Starting giiwa ..."

:checking
tasklist -v | findstr "java.exe" > NUL  
if ERRORLEVEL 1 goto starting
if ERRORLEVEL 0 goto sleep

goto sleep

:starting
call bin/startup.bat
ping 0.0.0.0 -n 3 >NUL

:sleep
ping 0.0.0.0 -n 1 >NUL

goto checking