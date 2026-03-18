from kivy.metrics import dp
from kivy.graphics import Color, RoundedRectangle
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.popup import Popup
from kivy.uix.textinput import TextInput


def refresh_contacts_list_view(app, ButtonContainer, ModernButton, COLOR_CARD):
    app.c_ls.clear_widgets()
    sv = app.ti_cs.text.lower()
    sv_workplace = app.ti_cs_workplace.text.lower() if hasattr(app, 'ti_cs_workplace') else ""
    sv_city = app.ti_cs_city.text.lower() if hasattr(app, 'ti_cs_city') else ""
    rows = app.conn.execute("SELECT name, surname, email, pesel, phone, workplace, apartment, notes FROM contacts ORDER BY surname ASC").fetchall()
    for d in rows:
        searchable = f"{d[0]} {d[1]} {d[2]} {d[4]} {d[5]} {d[6]} {d[7]}".lower()
        if sv and sv not in searchable:
            continue
        if sv_workplace and sv_workplace not in str(d[5]).lower():
            continue
        if sv_city and sv_city not in str(d[6]).lower():
            continue

        card = BoxLayout(orientation="vertical", size_hint_y=None, height=dp(250), padding=dp(10), spacing=dp(8))
        with card.canvas.before:
            Color(*COLOR_CARD)
            rect = RoundedRectangle(pos=card.pos, size=card.size, radius=[dp(12)])
        card.bind(pos=lambda inst, val, r=rect: setattr(r, 'pos', val), size=lambda inst, val, r=rect: setattr(r, 'size', val))

        name_lbl = Label(text=f"{d[0]} {d[1]}".title(), bold=True, halign="left", valign='middle', size_hint_y=None, height=dp(38))
        name_lbl.bind(size=lambda inst, val: setattr(inst, 'text_size', (inst.width - dp(6), None)))
        card.add_widget(name_lbl)

        info_text = (
            f"E: {d[2]}\n"
            f"PESEL: {d[3] if d[3] else '-'}\n"
            f"T: {d[4] if d[4] else '-'}\n"
            f"Zakład: {d[5] if d[5] else '-'}\n"
            f"Adres: {d[6] if d[6] else '-'}\n"
            f"Notatka: {d[7] if d[7] else '-'}"
        )
        info_lbl = Label(text=info_text, font_size='12sp', halign="left", valign='top', color=(0.84, 0.86, 0.92, 1))
        info_lbl.bind(size=lambda inst, val: setattr(inst, 'text_size', (inst.width - dp(6), None)))
        card.add_widget(info_lbl)

        actions = ButtonContainer(orientation='horizontal', size_hint_y=None, height=dp(60), min_button_width=dp(132), min_button_height=dp(44))
        phone_txt = str(d[4]).strip() if d[4] else ""
        actions.add_action(ModernButton(text="Zadzwoń", on_press=lambda x, ph=phone_txt: app._call_contact(ph), bg_color=(0.16, 0.6, 0.3, 1)))
        actions.add_action(ModernButton(text="WhatsApp", on_press=lambda x, ph=phone_txt, nm=d[0]: app._whatsapp_contact(ph, nm), bg_color=(0.06, 0.55, 0.25, 1)))
        actions.add_action(ModernButton(text="Edytuj", on_press=lambda x, data=d: app.form_contact(*data)))
        actions.add_action(ModernButton(text="Usuń", bg_color=(0.8, 0.2, 0.2, 1), on_press=lambda x, n=d[0], sn=d[1]: app.delete_contact(n, sn)))
        card.add_widget(actions)
        app.c_ls.add_widget(card)


def refresh_reports_list_view(app, Card, Label, PrimaryButton, COLOR_PRIMARY):
    app.r_grid.clear_widgets()
    rows = app.conn.execute("SELECT date, ok, fail, skip, details FROM reports ORDER BY id DESC").fetchall()
    for d, ok, fl, sk, det in rows:
        row = Card(orientation="vertical", size_hint_y=None, height=dp(120), padding=dp(10), spacing=dp(8))
        row.add_widget(Label(text=f"Sesja: {d}", bold=True, color=COLOR_PRIMARY))
        row.add_widget(Label(text=f"OK: {ok}  BŁĘDY: {fl}  POMINIĘTE: {sk}", color=(0.8, 0.85, 0.92, 1), size_hint_y=None, height=dp(26)))
        row.add_widget(PrimaryButton(text="Pokaż logi", size_hint_y=None, height=dp(42), on_press=lambda x, t=det: app.show_details(t)))
        app.r_grid.add_widget(row)


def show_report_details_popup(text):
    b = BoxLayout(orientation="vertical", padding=dp(10))
    ti = TextInput(text=str(text), readonly=True, font_size='11sp')
    b.add_widget(ti)
    b.add_widget(Button(text="ZAMKNIJ", size_hint_y=0.2, on_press=lambda x: p.dismiss()))
    p = Popup(title="Logi", content=b, size_hint=(.9, .8))
    p.open()
