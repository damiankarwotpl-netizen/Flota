# Native Android migration status

This repository now contains a native Android migration foundation in `android-native/` with:
- admin app module
- driver app module
- shared Room schema module
- shared feature contracts and navigation skeletons
- local driver-account handoff between admin car management and the driver app login/mileage/report flows
- native admin-side remote driver-account sync requests (create/reset/delete/assignment) with per-vehicle status visibility
- shared native driver remote gateway covering admin account sync, driver-side password reset parity, and admin-configurable remote endpoint settings
- driver session restore on app restart plus native admin-side endpoint validation for the production driver contract
- native driver mileage queue + periodic background sync worker, now posting real remote mileage updates with retry metadata and admin-side sync freshness visibility
- native driver notification channel for background mileage sync results and pending retry states
- hardened background sync scheduling/notifications plus timestamped database snapshot export safeguards and no-cleartext manifest defaults in both apps
- real PDF export of the vehicle report in both Android apps
- native SMTP send/test flow with saved templates, session-report/dashboard screens in admin, CSV export for session reports, special-send selection, pause/resume mass-mailing queue controls, and manual operator approval when auto-send is off
- the first clothes-module slices: native size management plus order headers/items/history, starter-order generation from workers/saved sizes, basic issue actions, and CSV/XLSX/PDF order + issue exports backed by Room

## Important
This is **not yet a finished 1:1 replacement** of the Python/Kivy application. It is the structural migration baseline required to continue safely.

## Next implementation blocks
The migration is now tracked as a **2-step remaining execution plan** in `docs/ANDROID_NATIVE_MIGRATION_BACKLOG.md`, covering:
1. SMTP pipeline hardening and rollout parity
2. QA, UAT, and release cutover
