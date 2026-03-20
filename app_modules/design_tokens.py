"""Wspólne tokeny UI dla nowocześniejszego wyglądu APK."""

from kivy.metrics import dp


def ui_tokens(mode: str) -> dict:
    dark = mode == "dark"
    if dark:
        return {
            "screen_bg": (0.05, 0.10, 0.09, 1),
            "screen_bg_alt": (0.07, 0.13, 0.11, 1),
            "card_bg": (0.09, 0.16, 0.14, 0.98),
            "card_bg_soft": (0.12, 0.21, 0.18, 0.98),
            "card_shadow": (0.00, 0.00, 0.00, 0.30),
            "primary": (0.10, 0.66, 0.46, 1),
            "primary_alt": (0.17, 0.76, 0.56, 1),
            "topbar_start": (0.04, 0.50, 0.38, 1),
            "topbar_end": (0.08, 0.66, 0.50, 1),
            "secondary": (0.14, 0.22, 0.20, 1),
            "input_bg": (0.08, 0.15, 0.13, 1),
            "success": (0.19, 0.73, 0.40, 1),
            "success_alt": (0.12, 0.59, 0.47, 1),
            "danger": (0.87, 0.28, 0.31, 1),
            "text": (0.93, 0.98, 0.95, 1),
            "muted": (0.67, 0.82, 0.74, 1),
            "border": (1, 1, 1, 0.10),
        }

    return {
        "screen_bg": (0.93, 0.98, 0.95, 1),
        "screen_bg_alt": (0.89, 0.96, 0.92, 1),
        "card_bg": (1.00, 1.00, 1.00, 1),
        "card_bg_soft": (0.95, 0.99, 0.97, 1),
        "card_shadow": (0.12, 0.29, 0.21, 0.12),
        "primary": (0.10, 0.66, 0.46, 1),
        "primary_alt": (0.16, 0.75, 0.54, 1),
        "topbar_start": (0.03, 0.59, 0.45, 1),
        "topbar_end": (0.07, 0.72, 0.54, 1),
        "secondary": (0.89, 0.96, 0.92, 1),
        "input_bg": (1.00, 1.00, 1.00, 1),
        "success": (0.14, 0.70, 0.42, 1),
        "success_alt": (0.10, 0.59, 0.46, 1),
        "danger": (0.87, 0.28, 0.31, 1),
        "text": (0.08, 0.15, 0.13, 1),
        "muted": (0.31, 0.45, 0.39, 1),
        "border": (0.08, 0.35, 0.24, 0.12),
    }


UI_RADIUS = {
    "sm": dp(16),
    "md": dp(22),
    "lg": dp(30),
}

UI_SPACING = {
    "xs": dp(8),
    "sm": dp(12),
    "md": dp(16),
    "lg": dp(22),
}
