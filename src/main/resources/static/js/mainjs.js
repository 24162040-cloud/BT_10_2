const feedback = document.getElementById('feedback');

async function api(path, body, authenticated = false) {
    const headers = {};
    if (body) headers['Content-Type'] = 'application/json';
    if (authenticated) headers.Authorization = `Bearer ${sessionStorage.getItem('token') || ''}`;
    const response = await fetch(path, {
        method: body ? 'POST' : 'GET', headers,
        ...(body && { body: JSON.stringify(body) })
    });
    const data = await response.json();
    if (!response.ok) {
        if (authenticated && response.status === 401) {
            sessionStorage.removeItem('token');
            location.replace('/login');
        }
        throw new Error(data.detail || 'Yêu cầu thất bại. Vui lòng thử lại.');
    }
    return data;
}

for (const action of ['login', 'signup']) {
    const form = document.getElementById(`${action}-form`);
    form?.addEventListener('submit', async event => {
        event.preventDefault();
        const button = form.querySelector('button');
        button.disabled = true;
        feedback.textContent = 'Đang xử lý…';
        try {
            const input = Object.fromEntries(new FormData(form));
            const data = await api(`/auth/${action}`, input);
            if (action === 'login') {
                // ponytail: token accessible to JS for this AJAX exercise; use HttpOnly cookies plus CSRF protection for a production browser app.
                sessionStorage.setItem('token', data.token);
                location.assign('/user/profile');
            } else {
                document.getElementById('email').value = input.email;
                form.reset();
                feedback.textContent = 'Đăng ký thành công. Hãy đăng nhập.';
                document.getElementById('password').focus();
            }
        } catch (error) {
            feedback.textContent = error.message;
        } finally {
            button.disabled = false;
        }
    });
}

if (document.getElementById('profile')) {
    if (!sessionStorage.getItem('token')) {
        location.replace('/login');
    } else {
        api('/users/me', null, true).then(user => {
            document.getElementById('fullName').textContent = user.fullName;
            document.getElementById('email').textContent = user.email;
            document.getElementById('profile').hidden = false;
        }).catch(error => { feedback.textContent = error.message; });
    }

    document.getElementById('logout').addEventListener('click', () => {
        sessionStorage.removeItem('token');
        location.replace('/login');
    });

    document.getElementById('load-users').addEventListener('click', async () => {
        try {
            const users = await api('/users', null, true);
            const rows = users.map(user => {
                const row = document.createElement('tr');
                for (const value of [user.id, user.fullName, user.email]) {
                    const cell = document.createElement('td');
                    cell.textContent = value;
                    row.append(cell);
                }
                return row;
            });
            document.getElementById('users').replaceChildren(...rows);
            document.getElementById('users-section').hidden = false;
            feedback.textContent = '';
        } catch (error) {
            feedback.textContent = error.message;
        }
    });
}
