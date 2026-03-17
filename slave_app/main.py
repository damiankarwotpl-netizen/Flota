import requests

from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.uix.textinput import TextInput
from kivy.uix.button import Button
from notification import start_weekly_notification

API_URL = https://script.google.com/macros/s/AKfycbxFQLZU-sg8Gg58J2dE-Bbt2jTyXrdcd1DOUM78vcqFLa789gpeOC9S4MyjGHpQ12_l/exec


class LoginScreen(BoxLayout):

    def __init__(self, app, **kwargs):
        super().__init__(orientation="vertical", **kwargs)

        self.app = app

        self.add_widget(Label(text="Login"))

        self.login = TextInput()
        self.add_widget(self.login)

        self.add_widget(Label(text="Password"))

        self.password = TextInput(password=True)
        self.add_widget(self.password)

        btn = Button(text="Login")
        btn.bind(on_press=self.do_login)

        self.add_widget(btn)

    def do_login(self, instance):

        r = requests.post(API_URL, json={

            "action": "login",
            "login": self.login.text,
            "password": self.password.text

        })

        data = r.json()

        if data["status"] == "ok":

            self.app.driver = data["name"]
            self.app.registration = data["registration"]

            if data["change_password"] == 1:

                self.app.root.clear_widgets()
                self.app.root.add_widget(ChangePassword(self.app))

            else:

                self.app.root.clear_widgets()
                self.app.root.add_widget(MileageScreen(self.app))


class ChangePassword(BoxLayout):

    def __init__(self, app, **kwargs):

        super().__init__(orientation="vertical", **kwargs)

        self.app = app

        self.add_widget(Label(text="Change password"))

        self.password = TextInput(password=True)
        self.add_widget(self.password)

        btn = Button(text="Save")
        btn.bind(on_press=self.change)

        self.add_widget(btn)

    def change(self, instance):

        requests.post(API_URL, json={

            "action": "change_password",
            "login": self.app.driver.lower().replace(" ", "."),
            "password": self.password.text

        })

        self.app.root.clear_widgets()
        self.app.root.add_widget(MileageScreen(self.app))


class MileageScreen(BoxLayout):

    def __init__(self, app, **kwargs):

        super().__init__(orientation="vertical", **kwargs)

        self.app = app

        self.add_widget(Label(text="Car"))

        self.add_widget(Label(text=self.app.registration))

        self.add_widget(Label(text="Mileage"))

        self.mileage = TextInput()
        self.add_widget(self.mileage)

        btn = Button(text="Save mileage")
        btn.bind(on_press=self.save)

        self.add_widget(btn)

    def save(self, instance):

        requests.post(API_URL, json={

            "action": "add_mileage",
            "driver": self.app.driver,
            "registration": self.app.registration,
            "mileage": int(self.mileage.text)

        })

        self.mileage.text = ""


class SlaveApp(App):

    driver = None
    registration = None

    def build(self):

        start_weekly_notification()

        root = BoxLayout()

        root.add_widget(LoginScreen(self))

        return root


if __name__ == "__main__":
    SlaveApp().run()
