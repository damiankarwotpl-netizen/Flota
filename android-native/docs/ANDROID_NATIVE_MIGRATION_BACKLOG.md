# Android native migration backlog

## Remaining steps to full migration

1. Finish remaining car workflows end-to-end, including remote driver-account API sync and mileage background sync.
2. Port workbook import, payroll import, table preview, and export logic.
3. Port SMTP send pipeline, attachments, special-send flow, mass mailing, and the remaining mailing actions.
4. Finish the remaining clothes workflow: CSV/XLSX exports and the remaining polish around the now-native order headers/items/history/issue slices.
5. Add real remote API wiring, notifications, background work, QA, and release hardening.

## Current count
**5 major implementation steps remain** after landing the local driver-account handoff, native vehicle-report PDF export, admin-side settings/report persistence, and the first clothes sizes/orders/items/history/issue slices.
