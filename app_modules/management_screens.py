from kivy.metrics import dp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.checkbox import CheckBox
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView
from kivy.uix.progressbar import ProgressBar


def setup_cars_screen(app, AppLayout, SecondaryButton, PrimaryButton, ModernInput):
    """Buduje ekran cars: nagłówek, lista i panel akcji."""
    app.sc_ref["cars"].clear_widgets()
    app.init_cars_db()

    shell = AppLayout(title="Samochody")
    shell.nav_tabs.add_action(SecondaryButton(text='Powrót', on_press=lambda x: setattr(app.sm, 'current', 'home')))
    shell.nav_tabs.add_action(PrimaryButton(text='+ DODAJ SAMOCHÓD', on_press=lambda x: app.add_car_popup(), size_hint_x=None, width=dp(210)))

    body = BoxLayout(orientation='vertical', spacing=dp(8))
    app.ti_cars_search = ModernInput(hint_text='Szukaj: nazwa / rejestracja / kierowca')
    app.ti_cars_search.bind(text=app.refresh_cars_list)
    body.add_widget(app.ti_cars_search)

    app.cars_grid = GridLayout(cols=1, spacing=dp(8), size_hint_y=None, padding=[dp(2), dp(2)])
    app.cars_grid.bind(minimum_height=app.cars_grid.setter('height'))
    sc = ScrollView()
    sc.add_widget(app.cars_grid)
    body.add_widget(sc)

    shell.set_content(body)
    shell.set_fab(lambda x: app.add_car_popup())
    app.sc_ref['cars'].add_widget(shell)
    app.refresh_cars_list()


def setup_workers_screen(app, AppLayout, SecondaryButton, PrimaryButton, ModernInput):
    app.sc_ref["pracownicy"].clear_widgets()
    shell = AppLayout(title="Pracownicy")
    shell.nav_tabs.add_action(SecondaryButton(text='Powrót', on_press=lambda x: setattr(app.sm, 'current', 'home')))
    shell.nav_tabs.add_action(PrimaryButton(text='Dodaj', on_press=lambda x: app.form_worker(), size_hint_x=None, width=dp(150)))
    body = BoxLayout(orientation='vertical', spacing=dp(8))
    app.ti_workers_search = ModernInput(hint_text='Szukaj pracownika (imię, nazwisko, zakład)')
    app.ti_workers_search.bind(text=app.refresh_workers_module)
    body.add_widget(app.ti_workers_search)
    app.workers_grid = GridLayout(cols=1, spacing=dp(8), size_hint_y=None)
    app.workers_grid.bind(minimum_height=app.workers_grid.setter('height'))
    sc = ScrollView()
    sc.add_widget(app.workers_grid)
    body.add_widget(sc)
    shell.set_content(body)
    shell.set_fab(lambda x: app.form_worker())
    app.sc_ref['pracownicy'].add_widget(shell)
    app.refresh_workers_module()


def setup_plants_screen(app, AppLayout, SecondaryButton, PrimaryButton, ModernInput):
    app.sc_ref["zaklady"].clear_widgets()
    shell = AppLayout(title="Zakłady")
    shell.nav_tabs.add_action(SecondaryButton(text='Powrót', on_press=lambda x: setattr(app.sm, 'current', 'home')))
    shell.nav_tabs.add_action(PrimaryButton(text='Dodaj', on_press=lambda x: app.form_plant(), size_hint_x=None, width=dp(150)))
    body = BoxLayout(orientation='vertical', spacing=dp(8))
    app.ti_plants_search = ModernInput(hint_text='Szukaj zakładu (nazwa, miasto, telefon)')
    app.ti_plants_search.bind(text=app.refresh_plants_list)
    body.add_widget(app.ti_plants_search)
    app.plants_grid = GridLayout(cols=1, spacing=dp(8), size_hint_y=None)
    app.plants_grid.bind(minimum_height=app.plants_grid.setter('height'))
    sc = ScrollView()
    sc.add_widget(app.plants_grid)
    body.add_widget(sc)
    shell.set_content(body)
    shell.set_fab(lambda x: app.form_plant())
    app.sc_ref['zaklady'].add_widget(shell)
    app.refresh_plants_list()


def setup_settings_screen(app, AppLayout, SecondaryButton, PrimaryButton):
    app.sc_ref["settings"].clear_widgets()
    shell = AppLayout(title="Ustawienia i narzędzia")
    shell.nav_tabs.add_action(SecondaryButton(text="Powrót", on_press=lambda x: setattr(app.sm, 'current', 'home')))

    body = BoxLayout(orientation="vertical", spacing=dp(10))
    try:
        contacts_count = app.conn.execute("SELECT COUNT(*) FROM contacts").fetchone()[0]
        workers_count = app.conn.execute("SELECT COUNT(*) FROM workers").fetchone()[0]
        cars_count = app.conn.execute("SELECT COUNT(*) FROM fleet_cars").fetchone()[0]
        plants_count = app.conn.execute("SELECT COUNT(*) FROM plants").fetchone()[0]
        body.add_widget(
            Label(
                text=f"Baza: kontakty {contacts_count} | pracownicy {workers_count} | auta {cars_count} | zakłady {plants_count}",
                size_hint_y=None,
                height=dp(34),
                color=(0.75, 0.82, 0.92, 1),
            )
        )
    except Exception:
        pass

    actions = ScrollView()
    action_grid = GridLayout(cols=1, spacing=dp(10), size_hint_y=None, padding=[dp(2), dp(2)])
    action_grid.bind(minimum_height=action_grid.setter('height'))
    action_grid.add_widget(PrimaryButton(text="Dodaj bazę danych", on_press=lambda x: app.open_picker("book"), height=dp(54), size_hint_y=None))
    action_grid.add_widget(PrimaryButton(text="Ustawienia SMTP", on_press=lambda x: setattr(app.sm, 'current', 'smtp'), height=dp(54), size_hint_y=None))
    action_grid.add_widget(PrimaryButton(text="Edytuj szablon email", on_press=lambda x: setattr(app.sm, 'current', 'tmpl'), height=dp(54), size_hint_y=None))
    action_grid.add_widget(PrimaryButton(text="Wczytaj arkusz płac", on_press=lambda x: app.open_picker("data"), height=dp(54), size_hint_y=None))
    action_grid.add_widget(SecondaryButton(text="Pokaż logi", on_press=app.show_logs, height=dp(54), size_hint_y=None))
    actions.add_widget(action_grid)
    body.add_widget(actions)
    shell.set_content(body)
    app.sc_ref["settings"].add_widget(shell)


def setup_paski_screen(app, AppLayout, Card, AppActionBar, SecondaryButton, PrimaryButton, DangerButton):
    app.sc_ref["paski"].clear_widgets()
    shell = AppLayout(title="Moduł Paski")
    shell.nav_tabs.add_action(SecondaryButton(text="Powrót", on_press=lambda x: setattr(app.sm, 'current', 'home')))

    body = BoxLayout(orientation="vertical", spacing=dp(10))
    auto_row = Card(orientation="horizontal", size_hint_y=None, height=dp(52), spacing=dp(10))
    app.cb_paski_auto = CheckBox(size_hint_x=None, width=dp(45))
    app.cb_paski_auto.active = app.auto_send_mode
    app.cb_paski_auto.bind(active=app.on_auto_checkbox_changed)
    auto_row.add_widget(app.cb_paski_auto)
    auto_row.add_widget(Label(text="AUTOMATYCZNA WYSYŁKA", bold=True))
    body.add_widget(auto_row)

    app.lbl_stats_paski = Label(text="Baza: 0 | Załączniki: 0", size_hint_y=None, height=dp(32))
    body.add_widget(app.lbl_stats_paski)
    app.pb_label_paski = Label(text="Gotowy", size_hint_y=None, height=dp(28))
    app.pb_paski = ProgressBar(max=100, size_hint_y=None, height=dp(24))
    body.add_widget(app.pb_label_paski)
    body.add_widget(app.pb_paski)

    actions = AppActionBar()
    actions.add_action(PrimaryButton(text="Wczytaj arkusz płac", on_press=lambda x: app.open_picker("data"), size_hint_x=None))
    actions.add_action(PrimaryButton(text="Podgląd i eksport", on_press=lambda x: [app.refresh_table(), setattr(app.sm, 'current', 'table')] if app.full_data else app.msg("!", "Wczytaj arkusz!"), size_hint_x=None))
    actions.add_action(PrimaryButton(text="Edytuj szablon", on_press=lambda x: setattr(app.sm, 'current', 'tmpl'), size_hint_x=None))
    actions.add_action(PrimaryButton(text="Dołącz załącznik", on_press=lambda x: app.open_picker("attachment"), size_hint_x=None))
    actions.add_action(PrimaryButton(text="Wyślij jeden plik", on_press=app.start_special_send_flow, size_hint_x=None))
    actions.add_action(PrimaryButton(text="Start masowa wysyłka", on_press=app.start_mass_mailing, size_hint_x=None))
    actions.add_action(SecondaryButton(text="PAUZA/RESUME", on_press=app.toggle_pause_mailing, size_hint_x=None))
    actions.add_action(SecondaryButton(text="Raporty sesji", on_press=lambda x: [app.refresh_reports(), setattr(app.sm, 'current', 'report')], size_hint_x=None))
    actions.add_action(DangerButton(text="Wyczyść załączniki", on_press=app.clear_all_attachments, size_hint_x=None))

    body.add_widget(actions)
    shell.set_content(body)
    app.sc_ref["paski"].add_widget(shell)
    app.update_stats()
