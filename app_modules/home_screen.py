from kivy.metrics import dp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView


MODULE_BUTTONS = [
    ("💬  Kontakty", "Czaty, telefon i WhatsApp", lambda app: [app.ensure_screen_ui("contacts"), app.refresh_contacts_list(), setattr(app.sm, 'current', 'contacts')], "primary"),
    ("🚗  Samochody", "Flota i szybkie akcje", lambda app: setattr(app.sm, 'current', 'cars'), "primary"),
    ("🧾  Raport auta", "Stan pojazdu i podsumowanie", lambda app: setattr(app.sm, 'current', 'vehicle_report'), "primary"),
    ("🦺  Ubranie", "Rozmiary i zamówienia", lambda app: setattr(app.sm, 'current', 'clothes'), "primary"),
    ("💳  Paski", "Płace i wysyłka", lambda app: setattr(app.sm, 'current', 'paski'), "primary"),
    ("👷  Pracownicy", "Kadry i dane", lambda app: setattr(app.sm, 'current', 'pracownicy'), "primary"),
    ("🏭  Zakłady", "Baza lokalizacji", lambda app: setattr(app.sm, 'current', 'zaklady'), "primary"),
    ("⚙️  Ustawienia", "Motyw i narzędzia", lambda app: setattr(app.sm, 'current', 'settings'), "secondary"),
]


def setup_home_screen(app, AppLayout, SecondaryButton, PrimaryButton, DangerButton):
    app.sc_ref["home"].clear_widgets()
    layout = AppLayout(title="Flota Messenger")
    layout.nav_tabs.add_action(SecondaryButton(text="🌞 Jasny", on_press=lambda x: app.switch_theme("light")))
    layout.nav_tabs.add_action(SecondaryButton(text="🌙 Ciemny", on_press=lambda x: app.switch_theme("dark")))

    content = BoxLayout(orientation="vertical", spacing=dp(10), padding=[dp(2), dp(8), dp(2), dp(8)])
    content.add_widget(Label(text="Czaty i moduły", bold=True, size_hint_y=None, height=dp(28), color=(0.88, 0.96, 0.92, 1)))
    content.add_widget(Label(text="Układ inspirowany WhatsApp • szybki dostęp do funkcji", size_hint_y=None, height=dp(22), color=(0.63, 0.80, 0.71, 1)))

    sv = ScrollView(size_hint=(1, 1))
    grid = GridLayout(cols=1, spacing=dp(10), padding=[dp(4), dp(4), dp(4), dp(10)], size_hint_y=None)
    grid.bind(minimum_height=grid.setter('height'))

    for title, subtitle, action, variant in MODULE_BUTTONS:
        button_cls = PrimaryButton if variant == "primary" else SecondaryButton
        grid.add_widget(
            button_cls(
                text=f"{title}\n{subtitle}",
                on_press=lambda x, handler=action: handler(app),
                size_hint_y=None,
                height=dp(88),
            )
        )

    sv.add_widget(grid)
    content.add_widget(sv)
    layout.set_content(content)
    app.sc_ref["home"].add_widget(layout)
