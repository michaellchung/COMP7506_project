@echo off
chcp 65001 >nul
echo ========================================
echo   NoteMind 启动脚本
echo ========================================
echo.

echo [1/3] 启动 Docker...
docker-compose up -d
if %errorlevel% neq 0 (
    echo Docker 启动失败，请确保 Docker Desktop 在运行
    pause
    exit /b 1
)
echo Docker 已启动

echo.
echo [2/3] 配置 ADB...
set "adb=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not exist "%adb%" set "adb=%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe"
"%adb%" reverse tcp:5000 tcp:5000
if %errorlevel% equ 0 (
    echo ADB 端口映射完成
) else (
    echo ADB 配置失败，请检查 USB 调试
)

echo.
echo [3/3] 启动后端...
start powershell.exe -NoExit -Command "cd backend; .venv\Scripts\activate; python app.py"

echo.
echo ========================================
echo 启动完成！
echo Backend URL: http://127.0.0.1:5000
echo ========================================
pause
