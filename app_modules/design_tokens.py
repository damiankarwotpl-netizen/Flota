"""Wspólne tokeny UI dla nowocześniejszego wyglądu APK."""

from kivy.metrics import dp


def ui_tokens(mode: str) -> dict:
    dark = mode == "dark"
    if dark:
        return {
            "screen_bg": (0.05, 0.07, 0.11, 1),
            "card_bg": (0.10, 0.13, 0.20, 1),
            "card_shadow": (0.00, 0.00, 0.00, 0.25),
            "primary": (0.24, 0.46, 0.90, 1),
            "primary_alt": (0.39, 0.58, 0.97, 1),
            "secondary": (0.18, 0.22, 0.32, 1),
            "success": (0.22, 0.72, 0.36, 1),
            "danger": (0.84, 0.26, 0.32, 1),
            "text": (0.94, 0.96, 0.99, 1),
            "muted": (0.67, 0.72, 0.82, 1),
            "border": (1, 1, 1, 0.12),
        }

    return {
        "screen_bg": (0.95, 0.96, 0.99, 1),
        "card_bg": (1.00, 1.00, 1.00, 1),
        "card_shadow": (0.18, 0.27, 0.45, 0.12),
        "primary": (0.23, 0.43, 0.86, 1),
        "primary_alt": (0.42, 0.63, 0.98, 1),
        "secondary": (0.90, 0.93, 0.98, 1),
        "success": (0.22, 0.72, 0.36, 1),
        "danger": (0.83, 0.24, 0.28, 1),
        "text": (0.13, 0.17, 0.24, 1),
        "muted": (0.43, 0.49, 0.58, 1),
        "border": (0.27, 0.39, 0.62, 0.16),
    }


UI_RADIUS = {
    "sm": dp(12),
    "md": dp(18),
    "lg": dp(24),
}

UI_SPACING = {
    "xs": dp(6),
    "sm": dp(10),
    "md": dp(14),
    "lg": dp(18),
}
