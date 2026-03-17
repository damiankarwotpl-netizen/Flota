import requests
import random
import string
from .config import API_URL


def generate_password(length=6):

    chars = string.digits

    return ''.join(random.choice(chars) for _ in range(length))


def generate_login(name):

    login = name.lower().replace(" ", ".")

    return login


def create_driver(name, registration):

    login = generate_login(name)

    password = generate_password()

    try:

        requests.post(
            API_URL,
            json={
                "action": "create_driver",
                "login": login,
                "password": password,
                "name": name,
                "registration": registration
            },
            timeout=10
        )

        return login, password

    except Exception as e:

        print("CREATE DRIVER ERROR:", e)

        return None, None
