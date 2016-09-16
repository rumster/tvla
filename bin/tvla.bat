@ECHO OFF

SET PROG=%1

set TVLA_HOME=%~dp0\..\

REM This allows for an unlimited number of command-line arguments
SET ARGS=
:SETUP_ARGS
IF %1a==a GOTO DONE_ARGS
SET ARGS=%ARGS% %1
SHIFT
GOTO SETUP_ARGS
:DONE_ARGS

java -Dtvla.home=%TVLA_HOME% -mx800m -jar %TVLA_HOME%\classes\artifacts\tvla3_jar\tvla3.jar %ARGS%

IF EXIST %PROG%.dt GOTO CREATE_POSTSCRIPT
GOTO CHECK_TR

:CREATE_POSTSCRIPT
ECHO Converting output to PostScript...
dot -Tps %PROG%.dt -o %PROG%.ps

:CHECK_TR
IF EXIST %PROG%.tr.dt GOTO CREATE_TR_POSTSCRIPT
GOTO EXIT

:CREATE_TR_POSTSCRIPT
ECHO Transition relation output to PostScript...
dot -Tps %PROG%.tr.dt -o %PROG%.tr.ps
GOTO EXIT

:EXIT