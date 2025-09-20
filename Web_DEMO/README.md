# 🏸 球友配對系統

一個基於 Python + Flask 的羽球俱樂部網站，提供用戶註冊、登入、球友配對和比賽記錄等功能。

## ✨ 功能特色

- 🔐 **用戶認證系統**：安全的註冊、登入和會話管理
- 👥 **球友配對**：根據位置、戰力和段位找到最適合的球友
- 📍 **地理位置服務**：支援 GPS 定位和距離計算
- 📊 **個人資料管理**：戰力、段位、勝率等統計資料
- 🏆 **比賽記錄**：追蹤比賽歷史和戰績統計
- 💬 **即時聊天**：球友間的即時通訊功能
- 📱 **響應式設計**：支援桌面和行動裝置

## 🚀 快速開始

### 1. 環境需求

- Python 3.7+
- pip (Python 套件管理器)

### 2. 安裝依賴

```bash
# 進入專案目錄
cd "YIF 黑客松"

# 安裝 Python 依賴套件
pip install -r requirements.txt
```

### 3. 設定環境

```bash
# 複製並編輯設定檔（可選）
cp .env.example .env
# 編輯 .env 檔案，設定資料庫連線等參數
```

### 4. 啟動應用程式

```bash
# 方法 1：使用啟動腳本（推薦）
python run.py

# 方法 2：直接啟動 Flask
python app.py
```

### 5. 開啟瀏覽器

在瀏覽器中開啟：http://localhost:8080
欲實施跨設備（僅限區域網）連結區先取得伺服器電腦IP,可輸入
```bash
ipconfig getifaddr en0
```
取得區域網後，可看到IP
與另一部設備的瀏覽器中輸入：http://IP位置：8080

## 🔑 預設帳號

系統會自動創建以下預設帳號：

| 用戶名 | 密碼 | 角色 | 段位 | 戰力 |
|--------|------|------|------|------|
| admin | admin123 | 管理員 | 大師 | 2500 |
| player1 | password123 | 一般用戶 | 黃金 | 1800 |
| player2 | password123 | 一般用戶 | 白金 | 2000 |
| player3 | password123 | 一般用戶 | 鑽石 | 2200 |

## 📁 專案結構

```
YIF 黑客松/
├── app.py                 # Flask 主應用程式
├── run.py                 # 啟動腳本
├── requirements.txt       # Python 依賴套件
├── README.md             # 專案說明文件
├── templates/            # HTML 模板
│   ├── login.html        # 登入頁面
│   ├── register.html     # 註冊頁面
│   └── dashboard.html    # 主控台頁面
├── static/               # 靜態檔案
│   └── images/          # 圖片檔案
│       └── badminton_player.png
└── badminton_club.db    # SQLite 資料庫（自動生成）
```

## 🛠️ 技術架構

### 後端技術
- **Flask**: Python Web 框架
- **SQLAlchemy**: ORM 資料庫操作
- **Flask-Login**: 用戶認證管理
- **SQLite**: 輕量級資料庫

### 前端技術
- **HTML5**: 語義化標記
- **CSS3**: 現代化樣式和動畫
- **JavaScript**: 互動功能和 API 調用
- **響應式設計**: 支援多種裝置

### 資料庫模型
- **User**: 用戶基本資料
- **Match**: 比賽記錄
- **UserLocation**: 用戶位置資訊

## 🔧 自訂設定

### 修改資料庫連線

在 `app.py` 中修改：

```python
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///your_database.db'
```

### 修改密鑰

在 `app.py` 中修改：

```python
app.config['SECRET_KEY'] = 'your-secret-key-here'
```

### 修改端口

在 `run.py` 中修改：

```python
app.run(debug=True, host='0.0.0.0', port=8080)
```

## 📱 API 端點

### 認證相關
- `POST /login` - 用戶登入
- `POST /register` - 用戶註冊
- `GET /logout` - 用戶登出

### 用戶資料
- `GET /api/profile` - 獲取個人資料
- `PUT /api/profile` - 更新個人資料
- `POST /api/location` - 更新位置資訊

### 球友配對
- `GET /api/players/nearby` - 獲取附近球友
- `POST /api/match/start` - 開始比賽
- `PUT /api/match/<id>/update` - 更新比賽記錄

## 🚀 部署建議

### 開發環境
- 使用 `python run.py` 啟動
- 啟用 debug 模式
- 使用 SQLite 資料庫

### 生產環境
- 使用 Gunicorn 或 uWSGI
- 設定 Nginx 反向代理
- 使用 PostgreSQL 或 MySQL
- 設定環境變數
- 啟用 HTTPS

## 🤝 貢獻指南

1. Fork 專案
2. 創建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交變更 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 開啟 Pull Request

## 📄 授權條款

本專案採用 MIT 授權條款 - 詳見 [LICENSE](LICENSE) 檔案 ~沒有這東西~

## 📞 聯絡資訊

如有問題或建議，請透過以下方式聯絡：

- 專案 Issues: [GitHub Issues](https://github.com/your-username/badminton-club/issues) ~沒有上傳~

## 製作

2025 YIF黑客松第14組 - 學測倒數146 所有成員


## 著作權

本專案著作權由 胡浩軒 謝卓樹 汪邦庭 魏子軒 李柚樂 陳孛宸 等人所有，嚴禁抄襲
特別鳴謝以上所有人

---
