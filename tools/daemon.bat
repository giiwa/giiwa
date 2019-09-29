@echo off
echo off
echo "giiwa daemon ..."

title giiwa Daemon
set startup=%~dp0
set startup=%startup:.=\.%

:checking
wmic process where caption="java.exe" get commandline /value | findstr  "%startup%" >NUL
if ERRORLEVEL 1 goto starting
if ERRORLEVEL 0 goto sleep

goto sleep

:starting
call %~dp0\bin\catalina.bat start
ping 0.0.0.0 -n 6 >NUL

:sleep
ping 0.0.0.0 -n 1 >NUL

goto checking