from kivy.metrics import dp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.checkbox import CheckBox
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView


def setup_vehicle_report_screen(app, AppLayout, SecondaryButton, PrimaryButton, ModernInput):
    app.sc_ref["vehicle_report"].clear_widgets()
    shell = AppLayout(title="Raport stanu samochodu")
    shell.nav_tabs.add_action(SecondaryButton(text="Wróć", on_press=lambda x: setattr(app.sm, 'current', 'home')))

    root = ScrollView()
    form = GridLayout(cols=1, spacing=dp(10), padding=dp(12), size_hint_y=None)
    form.bind(minimum_height=form.setter('height'))
    form.add_widget(Label(text="Raport stanu pojazdu • formularz", size_hint_y=None, height=dp(24), color=(0.72, 0.80, 0.92, 1), bold=True))

    app.vehicle_report_inputs = {}
    app.vehicle_report_checks = {}

    fields = [
        ("marka", "Marka"),
        ("rej", "Rejestracja"),
        ("przebieg", "Przebieg"),
        ("olej", "Poziom oleju"),
        ("paliwo", "Wskaźnik paliwa"),
        ("rodzaj_paliwa", "Rodzaj paliwa"),
        ("lp", "Lewy przedni"),
        ("pp", "Prawy przedni"),
        ("lt", "Lewy tylny"),
        ("pt", "Prawy tylny"),
        ("uszkodzenia", "Nowe uszkodzenia"),
        ("od_kiedy", "Od kiedy?"),
        ("serwis", "Przegląd / Service"),
        ("przeglad", "Przegląd techniczny"),
        ("uwagi", "Uwagi"),
    ]

    for key, hint in fields:
        multiline = key in {"uszkodzenia", "uwagi"}
        inp = ModernInput(hint_text=hint, multiline=multiline, size_hint_y=None, height=dp(84) if multiline else dp(48))
        app.vehicle_report_inputs[key] = inp
        form.add_widget(inp)

    checks = [
        ("trojkat", "Trójkąt"),
        ("kamizelki", "Kamizelki"),
        ("kolo", "Koło zapasowe"),
        ("dowod", "Dowód rejestracyjny"),
        ("apteczka", "Apteczka"),
    ]

    for key, label in checks:
        row = BoxLayout(size_hint_y=None, height=dp(42), spacing=dp(8))
        row.add_widget(Label(text=label, halign='left', valign='middle'))
        cb = CheckBox(size_hint=(None, None), size=(dp(38), dp(38)))
        app.vehicle_report_checks[key] = cb
        row.add_widget(cb)
        form.add_widget(row)

    actions = BoxLayout(size_hint_y=None, height=dp(56), spacing=dp(8))
    actions.add_widget(PrimaryButton(text="Zapisz PDF", on_press=app.save_vehicle_report_pdf))
    form.add_widget(actions)

    app.vehicle_report_status = Label(text="", size_hint_y=None, height=dp(48))
    form.add_widget(app.vehicle_report_status)

    root.add_widget(form)
    shell.set_content(root)
    app.sc_ref["vehicle_report"].add_widget(shell)
