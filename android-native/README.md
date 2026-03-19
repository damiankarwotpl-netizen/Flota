# Flota Native Android

This directory contains the native Android migration baseline for the legacy Kivy/KivyMD applications.

## Modules
- `app-admin` – native replacement target for `main.py`
- `app-driver` – native replacement target for `slave_app/main.py`
- `core-common` – shared navigation routes, UI state contracts, and repository interfaces
- `core-database` – Room entities, DAO, database definition, and local repository bootstrap matching the current SQLite schema

## Current state
The in-repo implementation backlog for the Android-native migration is closed.
Operational rollout is driven by the QA/UAT/release playbook in `docs/ANDROID_NATIVE_QA_UAT_RELEASE.md`.

## Remaining work
There are no remaining top-level implementation steps in `docs/ANDROID_NATIVE_MIGRATION_BACKLOG.md`.
Any follow-up work should come from issues discovered during execution of the rollout checklist.
