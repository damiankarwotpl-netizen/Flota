from kivy.app import App
from kivy.metrics import dp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.anchorlayout import AnchorLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView
try:
    from kivymd.uix.card import MDCard
    from kivymd.uix.label import MDLabel
except Exception:
    MDCard = None
    MDLabel = None


def setup_home_screen(app, AppLayout, SecondaryButton, PrimaryButton, DangerButton):
    app.sc_ref["home"].clear_widgets()
    layout = AppLayout(title="FUTURE ULTIMATE v20")
    layout.nav_tabs.add_action(SecondaryButton(text="Dark", on_press=lambda x: app.switch_theme("dark")))
    layout.nav_tabs.add_action(SecondaryButton(text="Light", on_press=lambda x: app.switch_theme("light")))

    content = BoxLayout(orientation="vertical", spacing=dp(12), padding=[0, dp(8), 0, 0])
    title_cls = MDLabel if MDLabel is not None else Label
    sub_cls = MDLabel if MDLabel is not None else Label
    content.add_widget(
        title_cls(
            text="Panel główny aplikacji",
            font_size='18sp',
            bold=True,
            color=(0.86, 0.90, 0.98, 1),
            size_hint_y=None,
            height=dp(32),
        )
    )
    sub = sub_cls(
        text="Wybierz moduł, aby kontynuować",
        font_size='13sp',
        color=(0.66, 0.73, 0.86, 1),
        size_hint_y=None,
        height=dp(24),
    )
    content.add_widget(sub)

    welcome = AnchorLayout(size_hint_y=None, height=dp(82))
    if MDCard is not None and MDLabel is not None:
        welcome_card = MDCard(
            size_hint=(1, None),
            height=dp(64),
            radius=[dp(16)] * 4,
            padding=[dp(12), 0, dp(12), 0],
            md_bg_color=(0.16, 0.25, 0.44, 1),
        )
        welcome_card.add_widget(
            MDLabel(
                text="Nowy wygląd UI • Funkcje bez zmian",
                halign="center",
                valign="middle",
                bold=True,
                theme_text_color="Custom",
                text_color=(0.93, 0.96, 1, 1),
            )
        )
    else:
        welcome_card = Label(
            text="Nowy wygląd UI • Funkcje bez zmian",
            size_hint=(1, None),
            height=dp(64),
            color=(0.93, 0.96, 1, 1),
            bold=True,
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
