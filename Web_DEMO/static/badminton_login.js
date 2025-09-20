// 顯示訊息函數
function showMessage(message, type) {
    const existingMessage = document.querySelector('.message-popup');
    if (existingMessage) existingMessage.remove();
    const messageDiv = document.createElement('div');
    messageDiv.className = `message-popup ${type}`;
    messageDiv.textContent = message;
    document.body.appendChild(messageDiv);
    setTimeout(() => { messageDiv.remove(); }, 3000);
}

// 密碼顯示切換功能
function togglePassword() {
    const passwordInput = document.getElementById('password');
    const toggleBtn = document.querySelector('.toggle-password');
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        toggleBtn.textContent = '隱藏';
    } else {
        passwordInput.type = 'password';
        toggleBtn.textContent = '顯示';
    }
}

// 初始化 UI 效果
document.addEventListener('DOMContentLoaded', function() {
    // 輸入框效果
    document.querySelectorAll('.form-input').forEach(input => {
        input.addEventListener('focus', () => { input.style.transform = 'scale(1.02)'; });
        input.addEventListener('blur', () => { input.style.transform = 'scale(1)'; });
        input.addEventListener('input', () => { input.style.borderColor = input.value ? '#4CAF50' : '#555'; });
    });

    // 按鈕縮放效果
    document.querySelectorAll('button').forEach(button => {
        button.addEventListener('click', function() {
            this.style.transform = 'scale(0.95)';
            setTimeout(() => { this.style.transform = 'scale(1)'; }, 150);
        });
    });

    // 標題按鈕
    const titleBtn = document.querySelector('.title-btn');
    if (titleBtn) {
        titleBtn.addEventListener('click', () => {
            showMessage('歡迎來到羽球俱樂部！', 'success');
        });
    }
});

// 鍵盤快捷鍵
document.addEventListener('keydown', function(e) {
    if (e.key === 'Enter') {
        const focused = document.activeElement;
        if (focused.classList.contains('form-input')) {
            e.preventDefault();
            const nextInput = focused.parentElement.nextElementSibling?.querySelector('.form-input');
            if (nextInput) nextInput.focus();
            else document.querySelector('.login-btn')?.click();
        }
    }
    if (e.ctrlKey && e.key === 'Enter') {
        e.preventDefault();
        document.querySelector('.login-btn')?.click();
    }
});

// 載入動畫
window.addEventListener('load', function() {
    const container = document.querySelector('.container');
    if (container) {
        container.style.opacity = '0';
        container.style.transform = 'translateY(20px)';
        container.style.transition = 'all 0.5s ease';
        setTimeout(() => {
            container.style.opacity = '1';
            container.style.transform = 'translateY(0)';
        }, 100);
    }
});

// 響應式
function handleResize() {
    const container = document.querySelector('.container');
    if (window.innerWidth <= 768) container.style.margin = '20px';
    else container.style.margin = '0';
}
window.addEventListener('resize', handleResize);
handleResize();
