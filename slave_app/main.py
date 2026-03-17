import requests

from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.textinput import TextInput

try:
    from .notification import start_weekly_notification
except Exception:
    from notification import start_weekly_notification

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
        try:
            data = api_post({
                "action": "login",
                "login": self.login.text.strip(),
                "password": self.password.text,
            })

            if data.get("status") != "ok":
                self.status.text = "Błędny login/hasło"
                return

            self.app.login = data.get("login", self.login.text.strip())
            self.app.driver = data.get("name", "")
            self.app.registration = data.get("registration", "")

            self.app.root.clear_widgets()
            if int(data.get("change_password", 0)) == 1:
                self.app.root.add_widget(ChangePassword(self.app))
            else:
                self.app.root.add_widget(MileageScreen(self.app))
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

            self.app.root.clear_widgets()
            self.app.root.add_widget(MileageScreen(self.app))
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
            api_post({
                "action": "add_mileage",
                "driver": self.app.login,
                "registration": self.app.registration,
                "mileage": mileage,
            })
            self.mileage.text = ""
            self.status.text = "Zapisano"
        except Exception as exc:
            self.status.text = f"Błąd zapisu: {str(exc)[:60]}"


class SlaveApp(App):
    login = None
    driver = None
    registration = None

    def build(self):
        start_weekly_notification()
        root = BoxLayout()
        root.add_widget(LoginScreen(self))
        return root


if __name__ == "__main__":
    SlaveApp().run()
