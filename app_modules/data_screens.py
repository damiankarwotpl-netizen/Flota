from kivy.metrics import dp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.gridlayout import GridLayout
from kivy.uix.screenmanager import ScreenManager, SlideTransition
from kivy.uix.scrollview import ScrollView


def setup_table_screen(app, AppLayout, SecondaryButton, PrimaryButton, ModernInput):
    app.sc_ref["table"].clear_widgets()
    shell = AppLayout(title="Podgląd i eksport")
    shell.nav_tabs.add_action(SecondaryButton(text="Wróć", on_press=lambda x: setattr(app.sm, 'current', 'paski')))
    shell.nav_tabs.add_action(PrimaryButton(text="Kolumny", on_press=app.popup_columns, size_hint_x=None, width=dp(150)))

    root = BoxLayout(orientation="vertical", spacing=dp(8))
    app.ti_tab_search = ModernInput(hint_text="Szukaj w tabeli...")
    app.ti_tab_search.bind(text=app.filter_table)
    root.add_widget(app.ti_tab_search)

    hs = ScrollView(size_hint_y=None, height=dp(58), do_scroll_y=False)
    app.table_header_layout = GridLayout(rows=1, size_hint=(None, None), height=dp(58))
    hs.add_widget(app.table_header_layout)

    ds = ScrollView(do_scroll_x=True, do_scroll_y=True)
    app.table_content_layout = GridLayout(size_hint=(None, None), spacing=dp(2))
    app.table_content_layout.bind(minimum_height=app.table_content_layout.setter('height'), minimum_width=app.table_content_layout.setter('width'))
    ds.add_widget(app.table_content_layout)
    ds.bind(scroll_x=lambda inst, val: setattr(hs, 'scroll_x', val))

    root.add_widget(hs)
    root.add_widget(ds)
    shell.set_content(root)
    app.sc_ref["table"].add_widget(shell)


def setup_clothes_screen(app, AppLayout, AppActionBar, SecondaryButton, PrimaryButton, ClothesSizesScreen, ClothesOrdersScreen, ClothesReportsScreen):
    app.sc_ref["clothes"].clear_widgets()
    shell = AppLayout(title="Ubranie robocze")
    shell.nav_tabs.add_action(SecondaryButton(text="Wróć", on_press=lambda x: setattr(app.sm, 'current', 'home')))

    tabs = AppActionBar()
    btn_w = dp(170)
    tabs.add_action(PrimaryButton(text="Rozmiary", size_hint_x=None, width=btn_w, on_press=lambda x: setattr(app.clothes_sm, 'current', 'sizes')))
    tabs.add_action(PrimaryButton(text="Zamówienia", size_hint_x=None, width=btn_w, on_press=lambda x: setattr(app.clothes_sm, 'current', 'orders')))
    tabs.add_action(PrimaryButton(text="Raporty", size_hint_x=None, width=btn_w, on_press=lambda x: setattr(app.clothes_sm, 'current', 'reports')))

    app.clothes_sm = ScreenManager(transition=SlideTransition())
    app.clothes_sm.add_widget(ClothesSizesScreen(name='sizes'))
    app.clothes_sm.add_widget(ClothesOrdersScreen(name='orders'))
    app.clothes_sm.add_widget(ClothesReportsScreen(name='reports'))
    app.clothes_sm.current = 'sizes'
    app._clothes_nav_bound = False
    app._bind_clothes_navigation()

    body = BoxLayout(orientation='vertical', spacing=dp(8))
    body.add_widget(tabs)
    body.add_widget(app.clothes_sm)
    shell.set_content(body)
    app.sc_ref["clothes"].add_widget(shell)
    app._push_nav_state()

    try:
        scr = app.clothes_sm.get_screen('sizes')
        if hasattr(scr, 'build_ui'):
            scr.build_ui()
        if hasattr(scr, 'refresh'):
            scr.refresh()
    except Exception:
        pass
