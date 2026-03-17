import random
import re
import string

import requests

from .config import API_URL


def generate_password(length=6):
    chars = string.digits
    return ''.join(random.choice(chars) for _ in range(length))


def generate_login(name):
    raw = str(name or '').strip().lower().replace(' ', '.')
    raw = re.sub(r'[^a-z0-9._-]', '', raw)
    raw = re.sub(r'\.{2,}', '.', raw).strip('.')
    return raw or 'driver'


def create_driver(name, registration):
    login = generate_login(name)
    password = generate_password()

    payload = {
        'action': 'create_driver',
        'login': login,
        'password': password,
        'name': name,
        'registration': registration,
    }

    try:
        response = requests.post(API_URL, json=payload, timeout=10)
        response.raise_for_status()
        data = response.json()

        if isinstance(data, dict) and data.get('status') not in (None, 'ok', 'created'):
            print('CREATE DRIVER ERROR: bad response:', data)
            return None, None

        return login, password
    except Exception as e:
        print('CREATE DRIVER ERROR:', e)
        return None, None
