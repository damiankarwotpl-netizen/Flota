from kivy.app import App
from kivy.metrics import dp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.anchorlayout import AnchorLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView


def setup_home_screen(app, AppLayout, Card, SecondaryButton, PrimaryButton, DangerButton):
    app.sc_ref["home"].clear_widgets()
    layout = AppLayout(title="FUTURE ULTIMATE v20")
    layout.nav_tabs.add_action(SecondaryButton(text="Dark", on_press=lambda x: app.switch_theme("dark")))
    layout.nav_tabs.add_action(SecondaryButton(text="Light", on_press=lambda x: app.switch_theme("light")))

    content = BoxLayout(orientation="vertical", spacing=dp(12), padding=[0, dp(8), 0, 0])
    hero_card = Card(orientation="vertical", size_hint_y=None, height=dp(120), spacing=dp(6), padding=dp(14))
    title = Label(
        text="Panel główny aplikacji",
        font_size='20sp',
        bold=True,
        color=(0.86, 0.90, 0.98, 1),
        size_hint_y=None,
        height=dp(34),
        halign="left",
        valign="middle",
    )
    title.bind(size=lambda inst, _: setattr(inst, "text_size", (inst.width - dp(4), None)))
    hero_card.add_widget(title)
    subtitle = Label(
        text="Wybierz moduł, aby kontynuować",
        font_size='13sp',
        color=(0.66, 0.73, 0.86, 1),
        size_hint_y=None,
        height=dp(24),
        halign="left",
        valign="middle",
    )
    subtitle.bind(size=lambda inst, _: setattr(inst, "text_size", (inst.width - dp(4), None)))
    hero_card.add_widget(subtitle)
    hero_card.add_widget(Label(text="Nowy układ UI • pełna funkcjonalność", color=(0.78, 0.85, 0.96, 1), bold=True, halign="left"))
    content.add_widget(hero_card)

    welcome = AnchorLayout(size_hint_y=None, height=dp(82))
    welcome_card = Card(size_hint=(1, None), height=dp(64), padding=dp(10))
    welcome_card.add_widget(
        Label(
            text="Nowy wygląd UI • Funkcje bez zmian",
            color=(0.93, 0.96, 1, 1),
            bold=True,
            halign="center",
            valign="middle",
        )
    )
    welcome.add_widget(welcome_card)
    content.add_widget(welcome)

    sv = ScrollView(size_hint=(1, 1))
    grid = GridLayout(cols=2, spacing=dp(14), padding=[dp(8), dp(4), dp(8), dp(10)], size_hint_y=None)
    grid.bind(minimum_height=grid.setter('height'))
    btn_props = dict(size_hint_y=None, height=dp(92))
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
