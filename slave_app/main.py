import os
import time

import requests

from kivy.app import App
from kivy.storage.jsonstore import JsonStore
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.textinput import TextInput

try:
    from .notification import MileageReminder
except Exception:
    from notification import MileageReminder

API_URL = "https://script.google.com/macros/s/AKfycbxFQLZU-sg8Gg58J2dE-Bbt2jTyXrdcd1DOUM78vcqFLa789gpeOC9S4MyjGHpQ12_l/exec"


def api_post(payload):
    response = requests.post(API_URL, json=payload, timeout=15)
    response.raise_for_status()
    return response.json()


class LoginScreen(BoxLayout):
    def __init__(self, app, **kwargs):
        super().__init__(orientation="vertical", **kwargs)
        self.app = app

        self.status = Label(text="")
        self.add_widget(Label(text="Login"))
        self.login = TextInput()
        self.add_widget(self.login)

        self.add_widget(Label(text="Password"))
        self.password = TextInput(password=True)
        self.add_widget(self.password)

        btn = Button(text="Login")
        btn.bind(on_press=self.do_login)
        self.add_widget(btn)
        self.add_widget(self.status)

    def do_login(self, _instance):
        login = self.login.text.strip()
        password = self.password.text
        try:
            data = api_post({
                "action": "login",
                "login": login,
                "password": password,
            })

            if data.get("status") != "ok":
                self.status.text = "Błędny login/hasło"
                return

            self.app.set_auth(
                login=data.get("login", login),
                password=password,
                driver=data.get("name", ""),
                registration=data.get("registration", ""),
            )

            if int(data.get("change_password", 0)) == 1:
                self.app.show_change_password()
            else:
                self.app.show_mileage_screen()
                self.app.ensure_reminder_started()
        except Exception as exc:
            self.status.text = f"Błąd połączenia: {str(exc)[:60]}"


class ChangePassword(BoxLayout):
    def __init__(self, app, **kwargs):
        super().__init__(orientation="vertical", **kwargs)
        self.app = app

        self.status = Label(text="")
        self.add_widget(Label(text="Change password"))
        self.password = TextInput(password=True)
        self.add_widget(self.password)

        btn = Button(text="Save")
        btn.bind(on_press=self.change)

        self.add_widget(btn)
        self.add_widget(self.status)

    def change(self, _instance):
        try:
            new_password = self.password.text.strip()
            if not new_password:
                self.status.text = "Hasło nie może być puste"
                return

            api_post({
                "action": "change_password",
                "login": self.app.login,
                "password": new_password,
            })

            self.app.update_password(new_password)
            self.app.show_mileage_screen()
            self.app.ensure_reminder_started()
        except Exception as exc:
            self.status.text = f"Błąd zmiany hasła: {str(exc)[:60]}"


class MileageScreen(BoxLayout):
    def __init__(self, app, **kwargs):
        super().__init__(orientation="vertical", **kwargs)
        self.app = app

        self.status = Label(text="")
        self.add_widget(Label(text="Car"))
        self.add_widget(Label(text=self.app.registration))

        self.add_widget(Label(text="Mileage"))
        self.mileage = TextInput()
        self.add_widget(self.mileage)

        btn = Button(text="Save mileage")
        btn.bind(on_press=self.save)

        self.add_widget(btn)
        self.add_widget(self.status)

    def save(self, _instance):
        try:
            mileage = int(self.mileage.text.strip())
            response = api_post({
                "action": "add_mileage",
                "driver": self.app.login,
                "registration": self.app.registration,
                "mileage": mileage,
            })

            if response.get("status") != "ok":
                self.status.text = response.get("message", "Błąd zapisu")
                return

            self.app.mark_mileage_updated()
            self.mileage.text = ""
            self.status.text = "Zapisano"
        except Exception as exc:
            self.status.text = f"Błąd zapisu: {str(exc)[:60]}"


class SlaveApp(App):
    login = None
    driver = None
    registration = None

    def build(self):
        self.root = BoxLayout()

        state_path = os.path.join(self.user_data_dir, 'slave_state.json')
        self.store = JsonStore(state_path)

        self.reminder = MileageReminder(self._get_state_value, self._set_state_value)

        auto_state = self._try_auto_login()
        if auto_state == 'mileage':
            self.show_mileage_screen()
            self.ensure_reminder_started()
        elif auto_state == 'change_password':
            self.show_change_password()
        else:
            self.show_login_screen()

        return self.root

    def _get_state_value(self, key, default=None):
        if self.store.exists('state'):
            return self.store.get('state').get(key, default)
        return default

    def _set_state_value(self, key, value):
        data = self.store.get('state') if self.store.exists('state') else {}
        data[key] = value
        self.store.put('state', **data)

    def set_auth(self, login, password, driver, registration):
        self.login = str(login or '').strip()
        self.driver = str(driver or '').strip()
        self.registration = str(registration or '').strip().upper()

        self._set_state_value('login', self.login)
        self._set_state_value('password', password)
        self._set_state_value('driver', self.driver)
        self._set_state_value('registration', self.registration)

        # start 7-day reminder window from first successful auth if not set yet
        if not self._get_state_value('last_mileage_update_ts', 0):
            self._set_state_value('last_mileage_update_ts', int(time.time()))
            self._set_state_value('last_notified_slot', -1)

    def update_password(self, password):
        self._set_state_value('password', password)

    def mark_mileage_updated(self):
        self.reminder.mark_mileage_updated()

    def ensure_reminder_started(self):
        self.reminder.start()

    def show_login_screen(self):
        self.root.clear_widgets()
        self.root.add_widget(LoginScreen(self))

    def show_change_password(self):
        self.root.clear_widgets()
        self.root.add_widget(ChangePassword(self))

    def show_mileage_screen(self):
        self.root.clear_widgets()
        self.root.add_widget(MileageScreen(self))

    def _try_auto_login(self):
        login = self._get_state_value('login', '').strip()
        password = self._get_state_value('password', '')
        if not login or not password:
            return 'none'

        try:
            data = api_post({
                'action': 'login',
                'login': login,
                'password': password,
            })
            if data.get('status') != 'ok':
                return 'none'
            if int(data.get('change_password', 0)) == 1:
                # wymagane ręczne ustawienie nowego hasła
                self.set_auth(login, password, data.get('name', ''), data.get('registration', ''))
                return 'change_password'

            self.set_auth(login, password, data.get('name', ''), data.get('registration', ''))
            return 'mileage'
        except Exception:
            return 'none'


if __name__ == "__main__":
    SlaveApp().run()
