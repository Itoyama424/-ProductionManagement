@echo off
setlocal enabledelayedexpansion

set OUT_DIR=merged
if not exist %OUT_DIR% mkdir %OUT_DIR%

REM ===== controller =====
set TARGET=src\main\java\com\production\management\controller
set OUTPUT=%OUT_DIR%\controller.txt
if exist "%OUTPUT%" del "%OUTPUT%"
for /r "%TARGET%" %%f in (*.java) do (
    echo FILE: %%f >> "%OUTPUT%"
    type "%%f" >> "%OUTPUT%"
)

REM ===== dto =====
set TARGET=src\main\java\com\production\management\dto
set OUTPUT=%OUT_DIR%\dto.txt
if exist "%OUTPUT%" del "%OUTPUT%"
for /r "%TARGET%" %%f in (*.java) do (
    echo FILE: %%f >> "%OUTPUT%"
    type "%%f" >> "%OUTPUT%"
)

REM ===== entity =====
set TARGET=src\main\java\com\production\management\entity
set OUTPUT=%OUT_DIR%\entity.txt
if exist "%OUTPUT%" del "%OUTPUT%"
for /r "%TARGET%" %%f in (*.java) do (
    echo FILE: %%f >> "%OUTPUT%"
    type "%%f" >> "%OUTPUT%"
)

REM ===== exception =====
set TARGET=src\main\java\com\production\management\exception
set OUTPUT=%OUT_DIR%\exception.txt
if exist "%OUTPUT%" del "%OUTPUT%"
for /r "%TARGET%" %%f in (*.java) do (
    echo FILE: %%f >> "%OUTPUT%"
    type "%%f" >> "%OUTPUT%"
)

REM ===== form =====
set TARGET=src\main\java\com\production\management\form
set OUTPUT=%OUT_DIR%\form.txt
if exist "%OUTPUT%" del "%OUTPUT%"
for /r "%TARGET%" %%f in (*.java) do (
    echo FILE: %%f >> "%OUTPUT%"
    type "%%f" >> "%OUTPUT%"
)

REM ===== mapper =====
set TARGET=src\main\java\com\production\management\mapper
set OUTPUT=%OUT_DIR%\mapper.txt
if exist "%OUTPUT%" del "%OUTPUT%"
for /r "%TARGET%" %%f in (*.java) do (
    echo FILE: %%f >> "%OUTPUT%"
    type "%%f" >> "%OUTPUT%"
)

REM ===== service/structure =====
set TARGET=src\main\java\com\production\management\service\structure
set OUTPUT=%OUT_DIR%\service_structure.txt
if exist "%OUTPUT%" del "%OUTPUT%"
for /r "%TARGET%" %%f in (*.java) do (
    echo FILE: %%f >> "%OUTPUT%"
    type "%%f" >> "%OUTPUT%"
)

echo Š®—¹
pause
