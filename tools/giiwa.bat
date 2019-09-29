@echo off

set daemon=%~dp0

if not "%JAVA_HOME%" == "" goto checking
echo ERROR, not set JAVA_HOME
goto end

:checking
wmic process where caption="daemon.exe" get commandline /value | findstr "%daemon%" >NUL
if ERRORLEVEL 1 goto starting
if ERRORLEVEL 0 goto started
goto end

:starting
echo %JAVA_HOME%
java -version
start %~dp0\daemon.exe
echo giiwa is started.
goto end

:started
echo giiwa already running. skipped

:end
