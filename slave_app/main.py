import os
import time
from pathlib import Path

import requests

from kivy.app import App
from kivy.storage.jsonstore import JsonStore
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView
from kivy.uix.textinput import TextInput
from kivy.uix.checkbox import CheckBox

try:
    from android.storage import app_storage_path
except Exception:
    app_storage_path = None

try:
    from .notification import MileageReminder
except Exception:
    from notification import MileageReminder

API_URL = "https://script.google.com/macros/s/AKfycbxFQLZU-sg8Gg58J2dE-Bbt2jTyXrdcd1DOUM78vcqFLa789gpeOC9S4MyjGHpQ12_l/exec"


def load_fpdf_class():
    import importlib

    module = importlib.import_module("fpdf")
    return module.FPDF


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

        protocol_btn = Button(text="Raport stanu samochodu")
        protocol_btn.bind(on_press=lambda _x: self.app.show_vehicle_report_screen())

        self.add_widget(btn)
        self.add_widget(protocol_btn)
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


class VehicleReportScreen(BoxLayout):
    def __init__(self, app, **kwargs):
        super().__init__(orientation="vertical", spacing=6, **kwargs)
        self.app = app
        self.status = Label(text="")

        self.inputs = {
            "marka": TextInput(hint_text="Marka", multiline=False),
            "rej": TextInput(text=self.app.registration, hint_text="Rejestracja", multiline=False),
            "przebieg": TextInput(hint_text="Przebieg", multiline=False),
            "olej": TextInput(hint_text="Poziom oleju", multiline=False),
            "paliwo": TextInput(hint_text="Wskaźnik paliwa", multiline=False),
            "rodzaj_paliwa": TextInput(hint_text="Rodzaj paliwa", multiline=False),
            "lp": TextInput(hint_text="Lewy przedni", multiline=False),
            "pp": TextInput(hint_text="Prawy przedni", multiline=False),
            "lt": TextInput(hint_text="Lewy tylny", multiline=False),
            "pt": TextInput(hint_text="Prawy tylny", multiline=False),
            "uszkodzenia": TextInput(hint_text="Nowe uszkodzenia"),
            "od_kiedy": TextInput(hint_text="Od kiedy?", multiline=False),
            "serwis": TextInput(hint_text="Przegląd / Service", multiline=False),
            "przeglad": TextInput(hint_text="Przegląd techniczny", multiline=False),
            "uwagi": TextInput(hint_text="Uwagi"),
        }
        self.checks = {
            "trojkat": CheckBox(),
            "kamizelki": CheckBox(),
            "kolo": CheckBox(),
            "dowod": CheckBox(),
            "apteczka": CheckBox(),
        }

        self.add_widget(Label(text="Raport stanu samochodu", size_hint_y=None, height=36))

        scroll = ScrollView()
        fields = BoxLayout(orientation="vertical", spacing=4, size_hint_y=None)
        fields.bind(minimum_height=fields.setter("height"))

        order = [
            "marka", "rej", "przebieg", "olej", "paliwo", "rodzaj_paliwa",
            "lp", "pp", "lt", "pt", "uszkodzenia", "od_kiedy", "serwis", "przeglad", "uwagi"
        ]
        for key in order:
            inp = self.inputs[key]
            inp.size_hint_y = None
            inp.height = 42 if not inp.multiline else 84
            fields.add_widget(inp)

        for key, label in [
            ("trojkat", "Trójkąt"),
            ("kamizelki", "Kamizelki"),
            ("kolo", "Koło zapasowe"),
            ("dowod", "Dowód rejestracyjny"),
            ("apteczka", "Apteczka"),
        ]:
            row = BoxLayout(size_hint_y=None, height=42)
            row.add_widget(Label(text=label))
            row.add_widget(self.checks[key])
            fields.add_widget(row)

        scroll.add_widget(fields)
        self.add_widget(scroll)

        actions = BoxLayout(size_hint_y=None, height=44, spacing=6)
        back_btn = Button(text="Wróć")
        back_btn.bind(on_press=lambda _x: self.app.show_mileage_screen())
        save_btn = Button(text="Zapisz PDF")
        save_btn.bind(on_press=self.save)
        actions.add_widget(back_btn)
        actions.add_widget(save_btn)
        self.add_widget(actions)
        self.add_widget(self.status)

    def save(self, _instance):
        data = {k: w.text for k, w in self.inputs.items()}
        data.update({k: w.active for k, w in self.checks.items()})
        try:
            output = self._generate_pdf(data)
            self.status.text = f"Zapisano: {output}"
        except ModuleNotFoundError:
            self.status.text = "Brak biblioteki fpdf - sprawdź zależności APK"
        except Exception as exc:
            self.status.text = f"Błąd PDF: {str(exc)[:60]}"

    def _generate_pdf(self, d):
        base_dir = Path(app_storage_path()) if app_storage_path else Path(self.app.user_data_dir)
        base_dir.mkdir(parents=True, exist_ok=True)
        file_path = base_dir / "protokol_stanu_pojazdu.pdf"

        FPDF = load_fpdf_class()
        pdf = FPDF(unit="pt", format="A4")
        pdf.set_auto_page_break(auto=False)
        pdf.add_page()

        W, H = 595.28, 841.89
        L, R = 40, W - 40
        y = H - 40

        pdf.set_font("Helvetica", "B", 14)
        pdf.text(L, H - y, "Miesięczny Protokół Stanu Pojazdu")
        y -= 20
        pdf.set_font("Helvetica", "", 9)

        def box(x, y0, w, h):
            pdf.rect(x, H - y0 - h, w, h)

        def vline(x, y1, y2):
            pdf.line(x, H - y1, x, H - y2)

        def txt(x, y0, t):
            pdf.text(x, H - y0, str(t) if t else "")

        def checkbox(x, y0, checked):
            box(x, y0, 10, 10)
            if checked:
                txt(x + 2, y0 + 8, "X")

        sec_top = y
        box(L, sec_top - 60, R - L, 60)
        v = L + (R - L) / 2
        vline(v, sec_top, sec_top - 60)
        txt(L + 5, sec_top - 15, "Marka:")
        txt(L + 80, sec_top - 15, d["marka"])
        txt(v + 5, sec_top - 15, "Rejestracja:")
        txt(v + 100, sec_top - 15, d["rej"])
        txt(L + 5, sec_top - 35, "Liczba miejsc:")
        txt(v + 5, sec_top - 35, "Wypełnione przez:")
        y = sec_top - 70

        box(L, y - 60, R - L, 60)
        vline(v, y, y - 60)
        txt(L + 5, y - 15, "Przebieg:")
        txt(L + 80, y - 15, d["przebieg"])
        txt(v + 5, y - 15, "Poziom oleju:")
        txt(v + 110, y - 15, d["olej"])
        txt(L + 5, y - 35, "Wskaźnik paliwa:")
        txt(L + 110, y - 35, d["paliwo"])
        txt(v + 5, y - 35, "Rodzaj paliwa:")
        txt(v + 110, y - 35, d["rodzaj_paliwa"])
        y -= 70

        box(L, y - 60, R - L, 60)
        vline(v, y, y - 60)
        txt(L + 5, y - 15, "Lewy przedni:")
        txt(L + 120, y - 15, d["lp"])
        txt(v + 5, y - 15, "Prawy przedni:")
        txt(v + 120, y - 15, d["pp"])
        txt(L + 5, y - 35, "Lewy tylny:")
        txt(L + 120, y - 35, d["lt"])
        txt(v + 5, y - 35, "Prawy tylny:")
        txt(v + 120, y - 35, d["pt"])
        y -= 70

        box(L, y - 80, R - L, 80)
        txt(L + 5, y - 15, "Bez uszkodzeń:")
        checkbox(L + 120, y - 18, False)
        txt(L + 150, y - 15, "TAK / NIE")
        txt(L + 5, y - 35, "Od kiedy:")
        txt(L + 120, y - 35, d["od_kiedy"])
        txt(L + 5, y - 55, "Nowe uszkodzenia:")
        txt(L + 150, y - 55, d["uszkodzenia"])
        y -= 90

        box(L, y - 80, R - L, 80)
        checkbox(L + 5, y - 20, d["trojkat"])
        txt(L + 20, y - 18, "Trójkąt")
        checkbox(L + 150, y - 20, d["kamizelki"])
        txt(L + 165, y - 18, "Kamizelki")
        checkbox(L + 300, y - 20, d["kolo"])
        txt(L + 315, y - 18, "Koło zapasowe")
        checkbox(L + 5, y - 50, d["dowod"])
        txt(L + 20, y - 48, "Dowód rejestracyjny")
        checkbox(L + 300, y - 50, d["apteczka"])
        txt(L + 315, y - 48, "Apteczka")
        y -= 90

        box(L, y - 50, R - L, 50)
        txt(L + 5, y - 20, "Przegląd / Service:")
        txt(L + 150, y - 20, d["serwis"])
        txt(L + 5, y - 40, "Przegląd techniczny:")
        txt(L + 150, y - 40, d["przeglad"])
        y -= 60

        box(L, y - 80, R - L, 80)
        txt(L + 5, y - 20, "Uwagi:")
        txt(L + 80, y - 20, d["uwagi"])
        y -= 100

        pdf.set_font("Helvetica", "", 8)
        txt(L, y, "Protokoły przekazywane w pierwszy poniedziałek miesiąca")
        pdf.output(str(file_path))
        return file_path


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

    def show_vehicle_report_screen(self):
        self.root.clear_widgets()
        self.root.add_widget(VehicleReportScreen(self))

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
