import json
from pathlib import Path

from kivy.metrics import dp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.checkbox import CheckBox
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.progressbar import ProgressBar
from kivy.uix.scrollview import ScrollView
try:
    from kivymd.uix.label import MDLabel
except Exception:
    MDLabel = None


def setup_email_screen(app, AppLayout, AppActionBar, Card, SecondaryButton, PrimaryButton, DangerButton):
    app.sc_ref["email"].clear_widgets()
    shell = AppLayout(title="Moduł Email")
    shell.nav_tabs.add_action(SecondaryButton(text="Wróć", on_press=lambda x: setattr(app.sm, 'current', 'home')))
    shell.nav_tabs.add_action(SecondaryButton(text="SMTP", on_press=lambda x: setattr(app.sm, 'current', 'smtp')))

    body = BoxLayout(orientation="vertical", spacing=dp(12), padding=[dp(4), dp(2), dp(4), dp(4)])
    title_cls = MDLabel if MDLabel is not None else Label
    body.add_widget(title_cls(text="Wysyłka i komunikacja", size_hint_y=None, height=dp(24), color=(0.72, 0.80, 0.92, 1), bold=True))
    auto_card = Card(orientation="horizontal", size_hint_y=None, height=dp(62), spacing=dp(10))
    app.cb_auto = CheckBox(size_hint_x=None, width=dp(45))
    app.cb_auto.active = app.auto_send_mode
    app.cb_auto.bind(active=app.on_auto_checkbox_changed)
    auto_card.add_widget(app.cb_auto)
    auto_card.add_widget(Label(text="AUTOMATYCZNA WYSYŁKA", bold=True))
    body.add_widget(auto_card)

    stats_card = Card(orientation="vertical", size_hint_y=None, height=dp(118), spacing=dp(8), padding=dp(12))
    stats_card.add_widget(Label(text="Status wysyłki", size_hint_y=None, height=dp(24), bold=True, halign="left"))
    app.lbl_stats = Label(text="Baza: 0", size_hint_y=None, height=dp(26), halign="left")
    stats_card.add_widget(app.lbl_stats)
    app.pb_label = Label(text="Gotowy", size_hint_y=None, height=dp(24), halign="left")
    app.pb = ProgressBar(max=100, size_hint_y=None, height=dp(24))
    stats_card.add_widget(app.pb_label)
    stats_card.add_widget(app.pb)
    body.add_widget(stats_card)

    actions = AppActionBar()
    actions.add_action(DangerButton(text="Wyczyść załączniki", on_press=app.clear_all_attachments, size_hint_x=None))
    actions.add_action(PrimaryButton(text="Edytuj szablon", on_press=lambda x: setattr(app.sm, 'current', 'tmpl'), size_hint_x=None))
    actions.add_action(PrimaryButton(text="Dodaj załącznik", on_press=lambda x: app.open_picker("attachment"), size_hint_x=None))
    actions.add_action(PrimaryButton(text="Wyślij jeden plik", on_press=app.start_special_send_flow, size_hint_x=None))
    actions.add_action(PrimaryButton(text="Start masowa wysyłka", on_press=app.start_mass_mailing, size_hint_x=None))
    actions.add_action(SecondaryButton(text="Pauza/Resume", on_press=app.toggle_pause_mailing, size_hint_x=None))

    actions_card = Card(orientation="vertical", size_hint_y=None, height=dp(96), padding=dp(6))
    actions_card.add_widget(actions)
    body.add_widget(actions_card)
    shell.set_content(body)
    app.sc_ref["email"].add_widget(shell)
    app.update_stats()


def setup_smtp_screen(app, AppLayout, AppActionBar, Card, SecondaryButton, PrimaryButton, ModernInput):
    app.sc_ref["smtp"].clear_widgets()
    p = Path(app.user_data_dir) / "smtp.json"
    d = json.load(open(p)) if p.exists() else {}

    shell = AppLayout(title="Ustawienia SMTP")
    shell.nav_tabs.add_action(SecondaryButton(text="Wróć", on_press=lambda x: setattr(app.sm, 'current', 'home')))

    form = BoxLayout(orientation="vertical", spacing=dp(12), padding=[dp(4), dp(2), dp(4), dp(4)])
    title_cls = MDLabel if MDLabel is not None else Label
    form.add_widget(title_cls(text="Konfiguracja serwera SMTP", size_hint_y=None, height=dp(24), color=(0.72, 0.80, 0.92, 1), bold=True))
    form_card = Card(orientation="vertical", spacing=dp(10), padding=dp(12), size_hint_y=None, height=dp(280))
    app.ti_h = ModernInput(hint_text="Host", text=d.get('h', ''))
    app.ti_pt = ModernInput(hint_text="Port", text=str(d.get('port', '587')))
    app.ti_u = ModernInput(hint_text="Email/Login", text=d.get('u', ''))
    app.ti_p = ModernInput(hint_text="Hasło/Klucz", password=True, text=d.get('p', ''))
    form_card.add_widget(app.ti_h)
    form_card.add_widget(app.ti_pt)
    form_card.add_widget(app.ti_u)
    form_card.add_widget(app.ti_p)
    form.add_widget(form_card)

    bx = Card(orientation="horizontal", size_hint_y=None, height=dp(52), spacing=dp(10))
    app.cb_b = CheckBox(size_hint_x=None, width=dp(45), active=d.get('batch', True))
    bx.add_widget(app.cb_b)
    bx.add_widget(Label(text="Batching (przerwa 60s/30 maili)"))
    form.add_widget(bx)

    actions = AppActionBar()
    actions.add_action(
        PrimaryButton(
            text="Zapisz",
            on_press=lambda x: [
                json.dump({'h': app.ti_h.text, 'port': app.ti_pt.text, 'u': app.ti_u.text, 'p': app.ti_p.text, 'batch': app.cb_b.active}, open(p, "w")),
                app.msg("OK", "Zapisano"),
            ],
            size_hint_x=None,
        )
    )
    actions.add_action(PrimaryButton(text="Test połączenia", on_press=lambda x: app.test_smtp_direct(), size_hint_x=None))
    actions.add_action(SecondaryButton(text="Pokaż logi", on_press=app.show_logs, size_hint_x=None))

    body = BoxLayout(orientation="vertical", spacing=dp(10))
    body.add_widget(form)
    body.add_widget(actions)
    shell.set_content(body)
    app.sc_ref["smtp"].add_widget(shell)


def setup_template_screen(app, AppLayout, AppActionBar, SecondaryButton, PrimaryButton, ModernInput):
    app.sc_ref["tmpl"].clear_widgets()
    ts = app.conn.execute("SELECT val FROM settings WHERE key='t_sub'").fetchone()
    tb = app.conn.execute("SELECT val FROM settings WHERE key='t_body'").fetchone()

    shell = AppLayout(title="Szablon email")
    shell.nav_tabs.add_action(SecondaryButton(text="Wróć", on_press=lambda x: setattr(app.sm, 'current', 'email')))

    form = BoxLayout(orientation="vertical", spacing=dp(12), padding=[dp(4), dp(2), dp(4), dp(4)])
    title_cls = MDLabel if MDLabel is not None else Label
    form.add_widget(title_cls(text="Edycja szablonu wiadomości", size_hint_y=None, height=dp(24), color=(0.72, 0.80, 0.92, 1), bold=True))
    form_card = Card(orientation="vertical", spacing=dp(10), padding=dp(12))
    ti_s = ModernInput(hint_text="Temat {Imię}", size_hint_y=None, height=dp(52))
    ti_b = ModernInput(hint_text="Treść...", multiline=True)
    ti_s.text, ti_b.text = (ts[0] if ts else ""), (tb[0] if tb else "")
    form_card.add_widget(ti_s)
    form_card.add_widget(ti_b)
    form.add_widget(form_card)

    actions = AppActionBar()
    actions.add_action(
        PrimaryButton(
            text="Zapisz",
            on_press=lambda x: [
                app.conn.execute("INSERT OR REPLACE INTO settings VALUES (?,?)", ('t_sub', ti_s.text)),
                app.conn.execute("INSERT OR REPLACE INTO settings VALUES (?,?)", ('t_body', ti_b.text)),
                app.conn.commit(),
                app.msg("OK", "Wzór zapisany"),
            ],
            size_hint_x=None,
        )
    )

    body = BoxLayout(orientation="vertical", spacing=dp(10))
    body.add_widget(form)
    body.add_widget(actions)
    shell.set_content(body)
    app.sc_ref["tmpl"].add_widget(shell)


def setup_contacts_screen(app, AppLayout, Card, SecondaryButton, PrimaryButton, ModernInput):
    app.sc_ref["contacts"].clear_widgets()
    shell = AppLayout(title="Kontakty")
    shell.nav_tabs.add_action(SecondaryButton(text="Wróć", on_press=lambda x: setattr(app.sm, 'current', 'home')))
    shell.nav_tabs.add_action(PrimaryButton(text="Dodaj", on_press=lambda x: app.form_contact(), size_hint_x=None, width=dp(150)))

    body = BoxLayout(orientation="vertical", spacing=dp(10), padding=[dp(4), dp(2), dp(4), dp(4)])
    title_cls = MDLabel if MDLabel is not None else Label
    body.add_widget(title_cls(text="Kontakty i filtry", size_hint_y=None, height=dp(24), color=(0.72, 0.80, 0.92, 1), bold=True))
    search_card = Card(orientation="vertical", size_hint_y=None, height=dp(136), spacing=dp(8), padding=dp(12))
    search_row = BoxLayout(size_hint_y=None, height=dp(54), spacing=dp(8))
    app.ti_cs = ModernInput(hint_text="Szukaj po imieniu, nazwisku, email, telefonie...")
    app.ti_cs.bind(text=app.refresh_contacts_list)
    search_row.add_widget(app.ti_cs)

    filter_row = BoxLayout(size_hint_y=None, height=dp(54), spacing=dp(8))
    app.ti_cs_workplace = ModernInput(hint_text="Filtr zakład pracy")
    app.ti_cs_workplace.bind(text=app.refresh_contacts_list)
    app.ti_cs_city = ModernInput(hint_text="Filtr adres / mieszkanie")
    app.ti_cs_city.bind(text=app.refresh_contacts_list)
    filter_row.add_widget(app.ti_cs_workplace)
    filter_row.add_widget(app.ti_cs_city)

    app.c_ls = GridLayout(cols=1, size_hint_y=None, spacing=dp(10), padding=[dp(2), dp(2)])
    app.c_ls.bind(minimum_height=app.c_ls.setter('height'))
    sc = ScrollView()
    sc.add_widget(app.c_ls)

    search_card.add_widget(search_row)
    search_card.add_widget(filter_row)
    body.add_widget(search_card)
    body.add_widget(sc)
    shell.set_content(body)
    shell.set_fab(lambda x: app.form_contact())
    app.sc_ref["contacts"].add_widget(shell)


def setup_report_screen(app, AppLayout, Card, SecondaryButton):
    app.sc_ref["report"].clear_widgets()
    shell = AppLayout(title="Historia sesji")
    shell.nav_tabs.add_action(SecondaryButton(text="Wróć", on_press=lambda x: setattr(app.sm, 'current', 'home')))

    root = BoxLayout(orientation="vertical", spacing=dp(10), padding=[dp(4), dp(2), dp(4), dp(4)])
    summary = Card(orientation="vertical", size_hint_y=None, height=dp(74), padding=dp(12))
    summary.add_widget(Label(text="Historia sesji i raporty z wysyłek", bold=True, halign="left", valign="middle"))
    root.add_widget(summary)

    app.r_grid = GridLayout(cols=1, size_hint_y=None, spacing=dp(10), padding=[dp(2), dp(2)])
    app.r_grid.bind(minimum_height=app.r_grid.setter('height'))
    sc = ScrollView()
    sc.add_widget(app.r_grid)
    root.add_widget(sc)
    shell.set_content(root)
    app.sc_ref["report"].add_widget(shell)
