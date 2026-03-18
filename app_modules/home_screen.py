from kivy.app import App
from kivy.metrics import dp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView


def setup_home_screen(app, AppLayout, SecondaryButton, PrimaryButton, DangerButton):
    app.sc_ref["home"].clear_widgets()
    layout = AppLayout(title="FUTURE ULTIMATE v20")
    layout.nav_tabs.add_action(SecondaryButton(text="Dark", on_press=lambda x: app.switch_theme("dark")))
    layout.nav_tabs.add_action(SecondaryButton(text="Light", on_press=lambda x: app.switch_theme("light")))

    content = BoxLayout(orientation="vertical", spacing=dp(10), padding=[0, dp(6), 0, 0])
    content.add_widget(
        Label(
            text="Panel główny aplikacji",
            font_size='15sp',
            color=(0.72, 0.78, 0.9, 1),
            size_hint_y=None,
            height=dp(26),
        )
    )
    sv = ScrollView(size_hint=(1, 1))
    grid = GridLayout(cols=2, spacing=dp(12), padding=dp(6), size_hint_y=None)
    grid.bind(minimum_height=grid.setter('height'))
    btn_props = dict(size_hint_y=None, height=dp(86))
    grid.add_widget(
        PrimaryButton(
            text="Kontakty",
            on_press=lambda x: [app.ensure_screen_ui("contacts"), app.refresh_contacts_list(), setattr(app.sm, 'current', 'contacts')],
            **btn_props,
        )
    )
    grid.add_widget(PrimaryButton(text="Samochody", on_press=lambda x: setattr(app.sm, 'current', 'cars'), **btn_props))
    grid.add_widget(PrimaryButton(text="Raport stanu auta", on_press=lambda x: setattr(app.sm, 'current', 'vehicle_report'), **btn_props))
    grid.add_widget(PrimaryButton(text="Ubranie robocze", on_press=lambda x: setattr(app.sm, 'current', 'clothes'), **btn_props))
    grid.add_widget(PrimaryButton(text="Paski", on_press=lambda x: setattr(app.sm, 'current', 'paski'), **btn_props))
    grid.add_widget(PrimaryButton(text="Pracownicy", on_press=lambda x: setattr(app.sm, 'current', 'pracownicy'), **btn_props))
    grid.add_widget(PrimaryButton(text="Zakłady", on_press=lambda x: setattr(app.sm, 'current', 'zaklady'), **btn_props))
    grid.add_widget(SecondaryButton(text="Ustawienia", on_press=lambda x: setattr(app.sm, 'current', 'settings'), **btn_props))
    grid.add_widget(DangerButton(text="Wyjście", on_press=lambda x: App.get_running_app().stop(), **btn_props))
    sv.add_widget(grid)
    content.add_widget(sv)
    layout.set_content(content)
    app.sc_ref["home"].add_widget(layout)
