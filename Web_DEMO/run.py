#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
羽球俱樂部 Flask 應用程式啟動腳本
"""

from app import app, db, User

def create_admin_user():
    """創建預設管理員帳號"""
    with app.app_context():
        admin = User.query.filter_by(username="admin").first()
        if not admin:
            admin = User(
                username='admin',
                email='admin@badminton.com',
                password='admin123',   # 直接存明碼
                nickname='管理員',
                rank='大師',
                rating=2500,
                win_rate=85.0,
                total_matches=100
            )
            db.session.add(admin)
            db.session.commit()
            print("✅ 管理員帳號已創建：admin / admin123")

def create_sample_users():
    """創建一些範例用戶"""
    with app.app_context():
        sample_users = [
            {
                'username': 'player1',
                'email': 'player1@badminton.com',
                'nickname': '小羽',
                'rank': '黃金',
                'rating': 1800,
                'win_rate': 65.0,
                'total_matches': 50
            },
            {
                'username': 'player2',
                'email': 'player2@badminton.com',
                'nickname': '阿寬',
                'rank': '白金',
                'rating': 2000,
                'win_rate': 70.0,
                'total_matches': 75
            },
            {
                'username': 'player3',
                'email': 'player3@badminton.com',
                'nickname': '魚丸',
                'rank': '鑽石',
                'rating': 2200,
                'win_rate': 75.0,
                'total_matches': 90
            }
        ]
        
        for u in sample_users:
            existing = User.query.filter_by(username=u['username']).first()
            if not existing:
                user = User(**u, password='password123')  # 統一密碼
                db.session.add(user)
                print(f"✅ 範例用戶已創建：{u['username']} / password123")
        
        db.session.commit()

if __name__ == '__main__':
    print("🏸 啟動羽球俱樂部應用程式...")
    
    with app.app_context():
        db.create_all()
        print("✅ 資料庫表格已創建")
        
        create_admin_user()
        create_sample_users()
    
    print("🚀 應用程式啟動中...")
    print("📱 請在瀏覽器中開啟：http://localhost:8080")
    print("🔑 預設管理員帳號：admin / admin123")
    print("👥 範例用戶帳號：player1 / password123")
    
    app.run(debug=True, host='0.0.0.0', port=8080)
