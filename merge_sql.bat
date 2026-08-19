@echo off
setlocal enabledelayedexpansion

REM ====== SQLファイルを探すフォルダを指定 ======
REM 例: src/main/resources や db など
set TARGET_DIR=.

REM ====== 出力ファイル名 ======
set OUTPUT_FILE=all_sql_sources.txt

REM ====== 既存の出力ファイルを削除 ======
if exist %OUTPUT_FILE% del %OUTPUT_FILE%

echo ==== SQL Source Merge Start ==== > %OUTPUT_FILE%

REM ====== 再帰的に .sql をすべて結合 ======
for /r %TARGET_DIR% %%f in (*.sql) do (
    echo. >> %OUTPUT_FILE%
    echo =============================================== >> %OUTPUT_FILE%
    echo FILE: %%f >> %OUTPUT_FILE%
    echo =============================================== >> %OUTPUT_FILE%
    type "%%f" >> %OUTPUT_FILE%
)

echo ==== Merge Completed ==== >> %OUTPUT_FILE%

echo 完了しました。出力: %OUTPUT_FILE%
pause
