"""Bridge między istniejącym AppTheme a KivyMD theme_cls."""


def apply_md_theme(app, app_theme_mode):
    """Synchronizuje theme_cls (KivyMD) z aktualnym trybem appki.

    Działa bezpiecznie również wtedy, gdy KivyMD nie jest aktywne.
    """
    if not hasattr(app, "theme_cls"):
        return

    app.theme_cls.theme_style = "Dark" if app_theme_mode == "dark" else "Light"
    app.theme_cls.primary_palette = "Blue"
    app.theme_cls.primary_hue = "500"
    app.theme_cls.accent_palette = "LightBlue"
