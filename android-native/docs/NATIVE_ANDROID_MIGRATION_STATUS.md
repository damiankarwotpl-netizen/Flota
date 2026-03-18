# Native Android migration status

This repository now contains a native Android migration foundation in `android-native/` with:
- admin app module
- driver app module
- shared Room schema module
- shared feature contracts and navigation skeletons
- local driver-account handoff between admin car management and the driver app login/mileage/report flows
- real PDF export of the vehicle report in both Android apps

## Important
This is **not yet a finished 1:1 replacement** of the Python/Kivy application. It is the structural migration baseline required to continue safely.

## Next implementation blocks
1. finish the remaining car workflows end-to-end, especially remote driver-account API sync and background mileage sync
2. workbook import/export adapters and payroll/table flows
3. SMTP + attachment pipeline
4. clothing workflow end-to-end persistence
5. remote API wiring, QA, notifications, and release hardening
