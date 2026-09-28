"""Run after mvn package. Stdlib only; isolated in-memory DB and a test-only key."""
import base64
import hashlib
import hmac
import json
import os
from pathlib import Path
import socket
import subprocess
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
SECRET = b'test-only-key-never-use-in-production-32'


def b64(data):
    return base64.urlsafe_b64encode(data).rstrip(b'=').decode()


def sign(claims, algorithm='HS256', key=SECRET):
    content = b64(json.dumps({'alg': algorithm, 'typ': 'JWT'}).encode()) + '.' + b64(json.dumps(claims).encode())
    digest = hashlib.sha384 if algorithm == 'HS384' else hashlib.sha256
    return content + '.' + b64(hmac.new(key, content.encode(), digest).digest())


def main():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        port = sock.getsockname()[1]
    base = f'http://127.0.0.1:{port}'
    java = str(Path(os.environ['JAVA_HOME']) / 'bin' / 'java.exe') if os.environ.get('JAVA_HOME') else 'java'
    env = dict(os.environ, JWT_SECRET=base64.b64encode(SECRET).decode())
    log_path = ROOT / 'target' / 'smoke-test.log'

    def request(path, expected=200, body=None, token=None):
        headers = {'Content-Type': 'application/json'}
        if token is not None:
            headers['Authorization'] = 'Bearer ' + token
        req = urllib.request.Request(base + path, headers=headers,
                                     data=json.dumps(body).encode() if body is not None else None)
        try:
            response = urllib.request.urlopen(req, timeout=5)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            text = response.read().decode()
            assert response.status == expected, (path, response.status, expected, text)
            assert not response.headers.get('Set-Cookie'), 'Stateless API must not create a session'
            return json.loads(text) if 'json' in response.headers.get('Content-Type', '') else text

    with log_path.open('w', encoding='utf-8') as log:
        process = subprocess.Popen([
            java, '-jar', str(ROOT / 'target' / 'jwt-nimbus-1.0.0.jar'),
            f'--server.port={port}', '--server.address=127.0.0.1',
            '--spring.datasource.url=jdbc:h2:mem:smoketest',
            '--spring.jpa.hibernate.ddl-auto=create-drop',
            '--security.jwt.expiration-time=3600000',
        ], cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 90
            while True:
                try:
                    assert 'JWT' in request('/login')
                    break
                except (urllib.error.URLError, TimeoutError):
                    if process.poll() is not None or time.monotonic() > deadline:
                        raise RuntimeError(f'Server did not start; see {log_path}')
                    time.sleep(.5)

            for path in ['/', '/user/profile', '/js/mainjs.js', '/css/style.css', '/images/avatar.svg']:
                request(path)
            for path in ['/users/me', '/users', '/users/']:
                request(path, 401)
            account = {'email': 'student@example.com', 'password': '123456', 'fullName': 'Nguyễn Văn An'}
            request('/auth/signup', 400, {})
            request('/auth/signup', 400, dict(account, email='invalid'))
            request('/auth/signup', 400, dict(account, password='ắ' * 30))
            registered = request('/auth/signup', body=account)
            assert registered['fullName'] == account['fullName'] and 'password' not in registered
            request('/auth/signup', 409, dict(account, email='STUDENT@example.com'))
            request('/auth/login', 401, dict(account, password='wrong'))
            request('/auth/login', 401, dict(account, email='missing@example.com'))
            login = request('/auth/login', body=dict(account, email='STUDENT@example.com'))
            token = login['token']
            header, payload, signature = token.split('.')
            assert json.loads(base64.urlsafe_b64decode(header + '=='))['alg'] == 'HS256'
            assert hmac.compare_digest(signature, b64(hmac.new(SECRET, f'{header}.{payload}'.encode(), hashlib.sha256).digest()))
            claims = json.loads(base64.urlsafe_b64decode(payload + '=='))
            assert login['expiresIn'] == (claims['exp'] - claims['iat']) * 1000 == 3600000
            assert claims['sub'] == account['email'] and 'password' not in claims
            assert request('/users/me', token=token)['id'] == registered['id']
            for path in ['/users', '/users/']:
                users = request(path, token=token)
                assert len(users) == 1 and 'password' not in users[0]
            invalid = [
                'bad-token', '', sign(claims, key=b'wrong-key'),
                sign(claims, algorithm='HS384'),
                header + '.' + payload + '.',
                sign(dict(claims, exp=int(time.time()) - 1)),
                sign({k: v for k, v in claims.items() if k != 'exp'}),
                sign(dict(claims, sub='')), sign(dict(claims, sub='missing@example.com')),
                sign(dict(claims, sub=123)), sign(dict(claims, iss='another-app')),
                sign(dict(claims, aud=['another-app'])),
                sign(dict(claims, nbf=int(time.time()) + 3600)),
                sign(dict(claims, iat=int(time.time()) + 3600)),
            ]
            for bad_token in invalid:
                request('/users/me', 401, token=bad_token)
            print('PASS: pages, signup, validation, duplicate email, login, HS256 signature, expiry, protected APIs, 14 invalid-token cases.')
        finally:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


if __name__ == '__main__':
    main()
