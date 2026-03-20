from kivy.metrics import dp
from kivy.graphics import Color, RoundedRectangle
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.popup import Popup
from kivy.uix.textinput import TextInput


WHATSAPP_GREEN = (0.10, 0.66, 0.46, 1)
WHATSAPP_GREEN_SOFT = (0.15, 0.74, 0.52, 1)
WHATSAPP_MUTED = (0.66, 0.76, 0.71, 1)
WHATSAPP_TEXT = (0.92, 0.97, 0.94, 1)


def _bind_rect(widget, rect):
    widget.bind(pos=lambda inst, val, r=rect: setattr(r, 'pos', val), size=lambda inst, val, r=rect: setattr(r, 'size', val))


def _make_badge(text, bg_color, text_color=WHATSAPP_TEXT):
    badge = Label(text=text, size_hint=(None, None), size=(dp(54), dp(54)), bold=True, color=text_color)
    with badge.canvas.before:
        Color(*bg_color)
        rect = RoundedRectangle(pos=badge.pos, size=badge.size, radius=[dp(27)])
    _bind_rect(badge, rect)
    return badge


def _make_meta_label(text, color, height):
    label = Label(text=text, size_hint_y=None, height=height, halign="left", valign="middle", color=color)
    label.bind(size=lambda inst, val: setattr(inst, 'text_size', (inst.width, None)))
    return label


def refresh_contacts_list_view(app, ButtonContainer, ModernButton, COLOR_CARD):
    app.c_ls.clear_widgets()
    sv = app.ti_cs.text.lower().strip() if hasattr(app, 'ti_cs') else ""
    rows = app.conn.execute("SELECT name, surname, email, pesel, phone, workplace, apartment, notes FROM contacts ORDER BY surname ASC").fetchall()
    for d in rows:
        searchable = f"{d[0]} {d[1]} {d[5]}".lower()
        if sv and sv not in searchable:
            continue

        full_name = f"{d[0]} {d[1]}".strip().title()
        initials = "".join(part[:1].upper() for part in full_name.split()[:2]) or "?"
        workplace = d[5] if d[5] else "Brak zakładu"
        phone_action = str(d[4]).strip() if d[4] else ""
        phone_txt = phone_action if phone_action else "Brak numeru"
        email_txt = d[2] if d[2] else "Brak e-maila"
        notes_txt = d[7] if d[7] else "Brak notatki"

        card = BoxLayout(orientation="vertical", size_hint_y=None, height=dp(180), padding=dp(12), spacing=dp(10))
        with card.canvas.before:
            Color(*COLOR_CARD)
            rect = RoundedRectangle(pos=card.pos, size=card.size, radius=[dp(20)])
        _bind_rect(card, rect)

        header = BoxLayout(orientation="horizontal", size_hint_y=None, height=dp(58), spacing=dp(12))
        header.add_widget(_make_badge(initials, WHATSAPP_GREEN))

        title_box = BoxLayout(orientation="vertical", spacing=dp(2))
        title_box.add_widget(_make_meta_label(full_name, WHATSAPP_TEXT, dp(28)))
        title_box.add_widget(_make_meta_label(workplace, WHATSAPP_MUTED, dp(22)))
        header.add_widget(title_box)
        card.add_widget(header)

        card.add_widget(_make_meta_label(f"📞 {phone_txt}", WHATSAPP_TEXT, dp(22)))
        card.add_widget(_make_meta_label(f"✉ {email_txt}", WHATSAPP_MUTED, dp(20)))
        card.add_widget(_make_meta_label(f"📝 {notes_txt}", WHATSAPP_MUTED, dp(20)))

        actions = ButtonContainer(orientation='horizontal', size_hint_y=None, height=dp(52), min_button_width=dp(110), min_button_height=dp(42))
        actions.add_action(ModernButton(text="Zadzwoń", on_press=lambda x, ph=phone_action: app._call_contact(ph), bg_color=WHATSAPP_GREEN_SOFT))
        actions.add_action(ModernButton(text="WhatsApp", on_press=lambda x, ph=phone_action, nm=d[0]: app._whatsapp_contact(ph, nm), bg_color=WHATSAPP_GREEN))
        actions.add_action(ModernButton(text="Edytuj", on_press=lambda x, data=d: app.form_contact(*data)))
        actions.add_action(ModernButton(text="Usuń", bg_color=(0.8, 0.2, 0.2, 1), on_press=lambda x, n=d[0], sn=d[1]: app.delete_contact(n, sn)))
        card.add_widget(actions)
        app.c_ls.add_widget(card)


def refresh_reports_list_view(app, Card, Label, PrimaryButton, COLOR_PRIMARY):
    app.r_grid.clear_widgets()
    rows = app.conn.execute("SELECT date, ok, fail, skip, details FROM reports ORDER BY id DESC").fetchall()
    for d, ok, fl, sk, det in rows:
        row = Card(orientation="vertical", size_hint_y=None, height=dp(128), padding=dp(12), spacing=dp(8))
        row.add_widget(Label(text=f"Sesja: {d}", bold=True, color=COLOR_PRIMARY))
        row.add_widget(Label(text=f"OK: {ok}  BŁĘDY: {fl}  POMINIĘTE: {sk}", color=(0.8, 0.85, 0.92, 1), size_hint_y=None, height=dp(26)))
        row.add_widget(PrimaryButton(text="Pokaż logi", size_hint_y=None, height=dp(44), on_press=lambda x, t=det: app.show_details(t)))
        app.r_grid.add_widget(row)


def show_report_details_popup(text):
    b = BoxLayout(orientation="vertical", padding=dp(10))
    ti = TextInput(text=str(text), readonly=True, font_size='11sp')
    b.add_widget(ti)
    b.add_widget(Button(text="ZAMKNIJ", size_hint_y=0.2, on_press=lambda x: p.dismiss()))
    p = Popup(title="Logi", content=b, size_hint=(.9, .8))
    p.open()
