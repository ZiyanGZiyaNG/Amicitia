#!/bin/bash

echo "🏸 啟動羽球俱樂部應用程式..."
echo

# 檢查Python是否安裝
if ! command -v python3 &> /dev/null; then
    echo "❌ 錯誤：未找到Python3，請先安裝Python 3.7+"
    exit 1
fi

echo "✅ Python3已安裝"
echo

# 檢查依賴套件
echo "📦 檢查依賴套件..."
if ! python3 -c "import flask" &> /dev/null; then
    echo "📥 安裝依賴套件..."
    pip3 install -r requirements.txt
    if [ $? -ne 0 ]; then
        echo "❌ 依賴套件安裝失敗"
        exit 1
    fi
    echo "✅ 依賴套件安裝完成"
else
    echo "✅ 依賴套件已安裝"
fi

echo
echo "🚀 啟動應用程式..."
echo "📱 請在瀏覽器中開啟：http://localhost:8080"
echo "🔑 預設管理員帳號：admin / admin123"
echo "👥 範例用戶帳號：player1 / password123"
echo
echo "按 Ctrl+C 停止應用程式"
echo

python3 run.py 