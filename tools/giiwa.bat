@echo off

if not "%JAVA_HOME%" == "" goto doStart
echo ERROR, not set JAVA_HOME
goto end

:doStart
echo %JAVA_HOME%
java -version
call %~dp0\daemon.exe
echo giiwa is started.

:end