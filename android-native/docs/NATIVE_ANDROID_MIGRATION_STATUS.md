# Native Android migration status

This repository now contains a native Android migration foundation in `android-native/` with:
- admin app module
- driver app module
- shared Room schema module
- shared feature contracts and navigation skeletons
- local driver-account handoff between admin car management and the driver app login/mileage/report flows
- native driver mileage queue + periodic background sync worker, with retry metadata and admin-side sync freshness visibility
- real PDF export of the vehicle report in both Android apps
- native SMTP send/test flow with saved templates, session-report/dashboard screens in admin, CSV export for session reports, special-send selection, and pause/resume mass-mailing queue controls
- the first clothes-module slices: native size management plus order headers/items/history, starter-order generation from workers/saved sizes, basic issue actions, and CSV/XLSX/PDF order + issue exports backed by Room

## Important
This is **not yet a finished 1:1 replacement** of the Python/Kivy application. It is the structural migration baseline required to continue safely.

## Next implementation blocks
The migration is now tracked as a **5-step execution plan** in `docs/ANDROID_NATIVE_MIGRATION_BACKLOG.md`, covering:
1. real remote driver-account sync
2. SMTP pipeline hardening and rollout parity
3. remaining remote API wiring
4. notifications and operational hardening
5. QA, UAT, and release cutover
