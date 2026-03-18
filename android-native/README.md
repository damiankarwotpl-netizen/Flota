# Flota Native Android

This directory contains the native Android migration baseline for the legacy Kivy/KivyMD applications.

## Modules
- `app-admin` – native replacement target for `main.py`
- `app-driver` – native replacement target for `slave_app/main.py`
- `core-common` – shared navigation routes, UI state contracts, and repository interfaces
- `core-database` – Room entities, DAO, database definition, and local repository bootstrap matching the current SQLite schema

## Current state
This is a structural migration baseline, not yet a finished 1:1 production replacement.
The intent is to establish the Android-native architecture and source layout required to port the remaining features safely.

## Remaining work
See `docs/ANDROID_NATIVE_MIGRATION_BACKLOG.md` for the current step-by-step backlog.
