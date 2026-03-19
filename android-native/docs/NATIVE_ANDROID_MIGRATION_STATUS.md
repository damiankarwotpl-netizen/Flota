# Native Android migration status

This repository now contains a native Android migration foundation in `android-native/` with:
- admin app module
- driver app module
- shared Room schema module
- shared feature contracts and navigation skeletons
- local driver-account handoff between admin car management and the driver app login/mileage/report flows
- real PDF export of the vehicle report in both Android apps
- local SMTP/template persistence plus native session-report/dashboard screens in admin, including CSV export for session reports and clothes counters in settings
- the first clothes-module slices: native size management plus order headers/items/history, starter-order generation from workers/saved sizes, basic issue actions, and CSV/XLSX exports backed by Room

## Important
This is **not yet a finished 1:1 replacement** of the Python/Kivy application. It is the structural migration baseline required to continue safely.

## Next implementation blocks
The migration is now tracked as a **10-step execution plan** in `docs/ANDROID_NATIVE_MIGRATION_BACKLOG.md`, covering:
1. remaining car-management parity
2. real remote driver-account sync
3. driver mileage/background sync
4. workbook import
5. payroll/table preview/export parity
6. SMTP pipeline and mailing actions
7. final clothes-flow parity
8. remaining remote API wiring
9. notifications and operational hardening
10. QA, UAT, and release cutover
