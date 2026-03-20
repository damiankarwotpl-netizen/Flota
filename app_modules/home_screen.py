from kivy.metrics import dp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.scrollview import ScrollView


MODULE_BUTTONS = [
    ("📇\nKontakty", lambda app: [app.ensure_screen_ui("contacts"), app.refresh_contacts_list(), setattr(app.sm, 'current', 'contacts')], "primary"),
    ("🚗\nSamochody", lambda app: setattr(app.sm, 'current', 'cars'), "primary"),
    ("🧾\nRaport auta", lambda app: setattr(app.sm, 'current', 'vehicle_report'), "primary"),
    ("🦺\nUbranie", lambda app: setattr(app.sm, 'current', 'clothes'), "primary"),
    ("💳\nPaski", lambda app: setattr(app.sm, 'current', 'paski'), "primary"),
    ("👷\nPracownicy", lambda app: setattr(app.sm, 'current', 'pracownicy'), "primary"),
    ("🏭\nZakłady", lambda app: setattr(app.sm, 'current', 'zaklady'), "primary"),
    ("⚙️\nUstawienia", lambda app: setattr(app.sm, 'current', 'settings'), "secondary"),
]


def setup_home_screen(app, AppLayout, SecondaryButton, PrimaryButton, DangerButton):
    app.sc_ref["home"].clear_widgets()
    layout = AppLayout(title="FUTURE ULTIMATE v20")
    layout.nav_tabs.add_action(SecondaryButton(text="Dark", on_press=lambda x: app.switch_theme("dark")))
    layout.nav_tabs.add_action(SecondaryButton(text="Light", on_press=lambda x: app.switch_theme("light")))

    content = BoxLayout(orientation="vertical", spacing=dp(12), padding=[0, dp(8), 0, dp(8)])
    sv = ScrollView(size_hint=(1, 1))
    grid = GridLayout(cols=2, spacing=dp(14), padding=[dp(8), dp(4), dp(8), dp(10)], size_hint_y=None)
    grid.bind(minimum_height=grid.setter('height'))
    btn_props = dict(size_hint_y=None, height=dp(120))

    for text, action, variant in MODULE_BUTTONS:
        button_cls = PrimaryButton if variant == "primary" else SecondaryButton
        grid.add_widget(button_cls(text=text, on_press=lambda x, handler=action: handler(app), **btn_props))

    sv.add_widget(grid)
    content.add_widget(sv)
    layout.set_content(content)
    app.sc_ref["home"].add_widget(layout)
