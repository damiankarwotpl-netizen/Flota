# Flota Native Android

This directory contains the native Android migration baseline for the legacy Kivy/KivyMD applications.

## Modules
- `app-admin` – native replacement target for `main.py`
- `app-driver` – native replacement target for `slave_app/main.py`
- `core-common` – shared navigation routes, UI state contracts, and repository interfaces
- `core-database` – Room entities, DAO, database definition, and local repository bootstrap matching the current SQLite schema

## Current state
Feature implementation is in place for the native migration baseline.
The remaining top-level work is operational: do the first build using `docs/ANDROID_NATIVE_FIRST_BUILD.md`, then execute the QA/UAT/release-cutover checklist in `docs/ANDROID_NATIVE_QA_UAT_RELEASE.md` and record sign-off with `docs/ANDROID_NATIVE_UAT_SIGNOFF_TEMPLATE.md`.

## Remaining work
See `docs/ANDROID_NATIVE_MIGRATION_BACKLOG.md` for the current final step and rollout status.
