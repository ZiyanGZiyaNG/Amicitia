from flask import Flask, render_template, request, redirect, url_for, flash
from flask_sqlalchemy import SQLAlchemy
from flask_login import LoginManager, login_user, login_required, logout_user, current_user, UserMixin

app = Flask(__name__)
app.config['SECRET_KEY'] = 'your-secret-key'
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///badminton_club.db'

db = SQLAlchemy(app)
login_manager = LoginManager(app)
login_manager.login_view = "login"

# =====================
# 資料庫模型
# =====================
class User(UserMixin, db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(150), unique=True, nullable=False)
    email = db.Column(db.String(150), unique=True, nullable=False)
    password = db.Column(db.String(150), nullable=False)
    nickname = db.Column(db.String(150))
    rank = db.Column(db.String(50), default="青銅")
    rating = db.Column(db.Integer, default=1000)
    win_rate = db.Column(db.Float, default=0.0)
    total_matches = db.Column(db.Integer, default=0)

@login_manager.user_loader
def load_user(user_id):
    return User.query.get(int(user_id))

# =====================
# 路由
# =====================

@app.route("/")
def index():
    return redirect(url_for("login"))

@app.route("/login", methods=["GET", "POST"])
def login():
    if request.method == "POST":
        username = request.form["username"]
        password = request.form["password"]
        user = User.query.filter_by(username=username, password=password).first()
        if user:
            login_user(user)
            flash("登入成功！", "success")
            # ⭐ 改這裡：登入成功直接進配對頁
            return redirect(url_for("match"))
        else:
            flash("帳號或密碼錯誤！", "error")
    return render_template("login.html")

@app.route("/register", methods=["GET", "POST"])
def register():
    if request.method == "POST":
        username = request.form["username"]
        email = request.form["email"]
        password = request.form["password"]
        confirm_password = request.form["confirm_password"]

        if password != confirm_password:
            flash("兩次密碼不一致！", "error")
            return redirect(url_for("register"))

        if User.query.filter_by(username=username).first():
            flash("使用者名稱已存在！", "error")
            return redirect(url_for("register"))

        new_user = User(username=username, email=email, password=password)
        db.session.add(new_user)
        db.session.commit()

        flash("註冊成功，請登入！", "success")
        return redirect(url_for("login"))
    return render_template("register.html")

@app.route("/logout")
@login_required
def logout():
    logout_user()
    flash("已登出！", "success")
    return redirect(url_for("login"))

# ⭐ 新增配對頁
@app.route("/match")
@login_required
def match():
    return render_template("match.html")  # 這就是原本的「羽球配對＋場地搜尋」頁，請改名存放在 templates/match.html

# 如果還想保留 dashboard，可以留著
@app.route("/dashboard")
@login_required
def dashboard():
    return render_template("dashboard.html")

# =====================
# 啟動
# =====================
if __name__ == "__main__":
    with app.app_context():
        db.create_all()
    app.run(host="0.0.0.0", port=8080, debug=True)
