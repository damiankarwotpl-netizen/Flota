# Android native migration backlog

## Remaining steps to full migration

1. Wire real repositories and app container into both Android apps.
2. Port contact/worker/plant CRUD persistence and sync rules end-to-end.
3. Port car workflows end-to-end, including driver-account API sync and mileage background sync.
4. Implement full vehicle-report PDF generation parity for admin and driver apps.
5. Port workbook import, payroll import, table preview, and export logic.
6. Port SMTP, attachments, special-send flow, mass mailing, and session reporting.
7. Port the full clothes module with sizes, order wizard, issue flows, CSV/XLSX exports, and yearly statistics.
8. Add real remote API wiring, notifications, background work, QA, and release hardening.

## Current count
**8 major implementation steps remain** after the structural baseline and shared state/repository contracts added so far.
