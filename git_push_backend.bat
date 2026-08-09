@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title Git 一键提交与推送（仅后端）

echo ========================================
echo    Git 一键提交与推送（仅后端）
echo ========================================
echo.

:: 0. 定义目录与远程仓库（仅后端）
set "BACKEND_DIR=c:\Users\admin\Desktop\新建文件夹 (4)\fgs2026-01-24-service-master"
set "REMOTE_NAME=lamumumuuu"
set "REMOTE_URL=https://github.com/lamumumuuu/FGS_CS_B.git"
set "BRANCH=main"

:: 1. 检查目录是否存在
if not exist "!BACKEND_DIR!\" (
    echo 错误：目录不存在：!BACKEND_DIR!
    pause
    exit /b 1
)

:: 1.5 检查GitHub连通性（新增）
echo [检测] 正在检查与GitHub的连接...
ping github.com -n 1 >nul
if errorlevel 1 (
    echo 错误：无法连接到 github.com，请检查网络。
    pause
    exit /b 1
)
echo 网络连接正常。

:: 2. 收集提交信息
call :input_msg
if errorlevel 1 goto end_error

:: 3. 处理后端
call :process_dir "!BACKEND_DIR!" "后端"
if errorlevel 1 goto end_error

echo.
echo ========================================
echo 全部完成！
echo ========================================
pause
exit /b 0

:: ========================================
:: 子程序：输入提交信息
:: ========================================
:input_msg
set "COMMIT_MSG="
set /p COMMIT_MSG="请输入提交信息: "
if "!COMMIT_MSG!"=="" (
    echo 提交信息不能为空！
    goto input_msg
)
exit /b 0

:: ========================================
:: 子程序：处理目录（add/commit/push）
:: ========================================
:process_dir
set "DIR=%~1"
set "NAME=%~2"
echo.
echo ========================================
echo 正在处理 !NAME! 目录: !DIR!
echo ========================================
cd /d "!DIR!"

:: 初始化 Git（如果尚未初始化）
if not exist ".git" (
    echo [配置] 初始化 Git 仓库...
    git init
    if errorlevel 1 (
        echo git init 失败！
        exit /b 1
    )
)

:: 配置远程 lamumumuuu
git remote get-url %REMOTE_NAME% >nul 2>&1
if errorlevel 1 (
    echo [配置] 添加远程 %REMOTE_NAME%...
    git remote add %REMOTE_NAME% %REMOTE_URL%
) else (
    for /f "delims=" %%u in ('git remote get-url %REMOTE_NAME%') do set "EXISTING_URL=%%u"
    if /i not "!EXISTING_URL!"=="%REMOTE_URL%" (
        echo [配置] 更新远程 URL...
        git remote set-url %REMOTE_NAME% %REMOTE_URL%
    )
)
echo 远程已配置: %REMOTE_NAME%

:: 添加所有更改
git add .
if errorlevel 1 (
    echo git add 失败！
    exit /b 1
)

:: 提交（允许空提交）
git status --porcelain | findstr . >nul
if errorlevel 1 (
    echo 工作区无更改，将创建空提交...
    git commit --allow-empty -m "!COMMIT_MSG!"
) else (
    git commit -m "!COMMIT_MSG!"
)
if errorlevel 1 (
    echo git commit 失败！
    exit /b 1
)
echo !NAME! 已提交

:: ---------- 智能推送（区分可重试与致命错误） ----------
:push_retry
echo [推送] 正在推送到 %REMOTE_NAME% %BRANCH%...
set "LOG_FILE=%TEMP%\git_push_output.log"
git push %REMOTE_NAME% %BRANCH% > "%LOG_FILE%" 2>&1
if not errorlevel 1 (
    echo 推送成功！
    del "%LOG_FILE%" 2>nul
    exit /b 0
)

:: 分析错误类型
set "FATAL=0"
set "TEMP_NET=0"

:: 致命错误关键词
type "%LOG_FILE%" | findstr /i "Repository not found" >nul && set "FATAL=1"
type "%LOG_FILE%" | findstr /i "remote: Repository not found" >nul && set "FATAL=1"
type "%LOG_FILE%" | findstr /i "Permission denied" >nul && set "FATAL=1"
type "%LOG_FILE%" | findstr /i "Authentication failed" >nul && set "FATAL=1"

:: 临时网络错误关键词（若出现则不视为致命）
type "%LOG_FILE%" | findstr /i "Recv failure" >nul && set "TEMP_NET=1"
type "%LOG_FILE%" | findstr /i "Connection reset" >nul && set "TEMP_NET=1"
type "%LOG_FILE%" | findstr /i "Connection timed out" >nul && set "TEMP_NET=1"
type "%LOG_FILE%" | findstr /i "Could not resolve host" >nul && set "TEMP_NET=1"

:: 若存在网络临时错误，则忽略致命标记，进入重试
if "!TEMP_NET!"=="1" set "FATAL=0"

if "!FATAL!"=="1" (
    echo ----------------------------------------
    echo 推送失败：检测到永久性错误，无法通过重试解决。
    echo 错误详情：
    type "%LOG_FILE%"
    echo ----------------------------------------
    echo 请检查仓库是否存在、地址是否正确、是否有推送权限。
    del "%LOG_FILE%" 2>nul
    pause
    exit /b 1
)

:: 临时性错误：显示错误并启动重试
echo 推送失败，可能是网络波动。按任意键取消，或等待自动重试...
type "%LOG_FILE%" | findstr /i "error fatal" >nul
set /a count=0
:wait_loop
if !count! geq 3 goto push_retry
timeout /t 1 >nul
if errorlevel 1 (
    echo 用户取消推送。
    del "%LOG_FILE%" 2>nul
    exit /b 1
)
echo 喵~ !count!/3
set /a count+=1
goto wait_loop

:: ========================================
:: 错误退出点
:: ========================================
:end_error
echo 操作过程中出现错误，已中止。
pause
exit /b 1