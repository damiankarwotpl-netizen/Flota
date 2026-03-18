from kivy.metrics import dp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.popup import Popup
from kivy.uix.scrollview import ScrollView
from kivy.uix.textinput import TextInput


def show_message_popup(title, text, PrimaryButton):
    b = BoxLayout(orientation="vertical", padding=dp(18), spacing=dp(10))
    l = Label(text=text, halign="center", valign="middle")
    l.bind(size=lambda inst, val: setattr(inst, 'text_size', (inst.width - dp(8), None)))
    b.add_widget(l)
    b.add_widget(PrimaryButton(text="OK", on_press=lambda x: p.dismiss(), height=dp(54), size_hint_y=None))
    p = Popup(title=title, content=b, size_hint=(0.92, 0.55), auto_dismiss=False)
    p.open()


def update_stats_labels(app):
    try:
        count = app.conn.execute('SELECT count(*) FROM contacts').fetchone()[0]
        s = f"Baza: {count} | Załączniki: {len(app.global_attachments)}"
        if hasattr(app, 'lbl_stats'):
            app.lbl_stats.text = s
        if hasattr(app, 'lbl_stats_paski'):
            app.lbl_stats_paski.text = s
    except Exception:
        pass


def update_progress_labels(app, done):
    try:
        val = int((done / app.total_q) * 100) if app.total_q else 0
        if hasattr(app, 'pb'):
            app.pb.value = val
        if hasattr(app, 'pb_paski'):
            app.pb_paski.value = val
        if hasattr(app, 'pb_label'):
            app.pb_label.text = f"Postęp: {done}/{app.total_q}"
        if hasattr(app, 'pb_label_paski'):
            app.pb_label_paski.text = f"Postęp: {done}/{app.total_q}"
    except Exception:
        pass


def popup_columns_selector(app, ModernButton, CheckBox):
    box = BoxLayout(orientation="vertical", padding=dp(10))
    gr = GridLayout(cols=1, size_hint_y=None, spacing=dp(5))
    checks = []
    gr.bind(minimum_height=gr.setter('height'))
    for i, h in enumerate(app.full_data[0]):
        r = BoxLayout(size_hint_y=None, height=dp(45))
        cb = CheckBox(active=(i in app.export_indices), size_hint_x=None, width=dp(50))
        checks.append((i, cb))
        r.add_widget(cb)
        r.add_widget(Label(text=str(h)))
        gr.add_widget(r)
    sc = ScrollView()
    sc.add_widget(gr)
    box.add_widget(sc)
    box.add_widget(
        ModernButton(
            text="ZASTOSUJ",
            on_press=lambda x: [
                setattr(app, 'export_indices', [idx for idx, c in checks if c.active]),
                p.dismiss(),
                app.refresh_table(),
            ],
            height=dp(50),
            size_hint_y=None,
        )
    )
    p = Popup(title="Kolumny", content=box, size_hint=(0.9, 0.9))
    p.open()


def show_logs_popup(app, Button):
    try:
        text = ""
        if app.log_file.exists():
            with open(app.log_file, "r", encoding="utf-8") as f:
                text = f.read()[-40000:]
        else:
            text = "\n".join(app._log_buffer)
        b = BoxLayout(orientation="vertical", padding=dp(10))
        ti = TextInput(text=text, readonly=True, font_size='11sp')
        b.add_widget(ti)
        b.add_widget(Button(text="ZAMKNIJ", size_hint_y=0.2, on_press=lambda x: p.dismiss()))
        p = Popup(title="Logi aplikacji", content=b, size_hint=(.95, .95))
        p.open()
    except Exception:
        app.log("show_logs error")
        app.msg("Błąd", "Nie można otworzyć logów")
