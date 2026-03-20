"""Bridge między istniejącym AppTheme a KivyMD theme_cls."""


def apply_md_theme(app, app_theme_mode):
    """Synchronizuje theme_cls (KivyMD) z aktualnym trybem appki.

    Działa bezpiecznie również wtedy, gdy KivyMD nie jest aktywne.
    """
    if not hasattr(app, "theme_cls"):
        return

    is_dark = app_theme_mode == "dark"
    app.theme_cls.theme_style = "Dark" if is_dark else "Light"
    app.theme_cls.primary_palette = "Green"
    app.theme_cls.primary_hue = "500"
    app.theme_cls.accent_palette = "Teal" if not is_dark else "LightGreen"
    app.theme_cls.accent_hue = "400"
