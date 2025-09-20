@echo off
echo 🏸 啟動羽球俱樂部應用程式...
echo.

REM 檢查Python是否安裝
python --version >nul 2>&1
if errorlevel 1 (
    echo ❌ 錯誤：未找到Python，請先安裝Python 3.7+
    pause
    exit /b 1
)

echo ✅ Python已安裝
echo.

REM 檢查依賴套件
echo 📦 檢查依賴套件...
pip show flask >nul 2>&1
if errorlevel 1 (
    echo 📥 安裝依賴套件...
    pip install -r requirements.txt
    if errorlevel 1 (
        echo ❌ 依賴套件安裝失敗
        pause
        exit /b 1
    )
    echo ✅ 依賴套件安裝完成
) else (
    echo ✅ 依賴套件已安裝
)

echo.
echo 🚀 啟動應用程式...
echo 📱 請在瀏覽器中開啟：http://localhost:8080
echo 🔑 預設管理員帳號：admin / admin123
echo 👥 範例用戶帳號：player1 / password123
echo.
echo 按 Ctrl+C 停止應用程式
echo.

python run.py

pause 