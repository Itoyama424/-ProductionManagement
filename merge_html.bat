@echo off
setlocal enabledelayedexpansion

set OUT_DIR=%~dp0merged
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"
set TARGET=src\main\resources\templates

for /r "%TARGET%" %%f in (*.html) do (
    set "NAME=%%~nf"
    set "OUTPUT=%OUT_DIR%\!NAME!.html.b64.txt"

    echo FILE: %%f > "!OUTPUT!"
    echo ----------------------------------------------- >> "!OUTPUT!"

    REM HTMLをBase64に変換して出力
    powershell -Command ^
      "$bytes = [System.IO.File]::ReadAllBytes('%%f'); " ^
      "$b64 = [System.Convert]::ToBase64String($bytes); " ^
      "$b64 | Out-File '!OUTPUT!' -Encoding ASCII"
)

echo 完了
pause
