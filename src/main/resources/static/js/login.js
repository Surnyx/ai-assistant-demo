const loginForm = document.querySelector('#login-form');
const loginButton = document.querySelector('#login-button');
const loginError = document.querySelector('#login-error');

loginForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    loginError.textContent = '';
    loginButton.disabled = true;
    loginButton.textContent = '登录中...';

    const username = document.querySelector('#username').value.trim();
    const password = document.querySelector('#password').value;

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) {
            const error = await readError(response);
            throw new Error(error);
        }

        window.location.replace('/chat.html');
    } catch (error) {
        loginError.textContent = error.message || '登录失败，请稍后重试';
        loginButton.disabled = false;
        loginButton.textContent = '登录';
    }
});

async function readError(response) {
    try {
        const data = await response.json();
        return data.message || '请求失败';
    } catch (error) {
        return '服务器暂时不可用';
    }
}

