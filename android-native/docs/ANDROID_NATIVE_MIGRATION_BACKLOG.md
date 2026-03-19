# Android native migration backlog

## Remaining steps to full migration

Below is the concrete **6-step plan** to reach a 1:1 native replacement of the legacy Kivy apps in this repository.

1. **Add real remote driver-account synchronization.**
   Replace the current local-only handoff with the production API contract for driver credentials, reset flows, and car-to-driver assignment sync.

2. **Implement mileage/background sync for the driver app.**
   Add periodic native background work for mileage refresh, offline queueing, retry handling, and admin-side visibility into sync freshness.

3. **Finish the SMTP pipeline end-to-end.**
   Move from local config/template persistence to real SMTP send, test connection, attachments, special-send flow, and mass mailing queue behavior now still living in the Kivy app.

4. **Wire the remaining remote API/integration surfaces.**
   Port the missing production integrations used by admin and driver flows so native Android becomes the primary runtime instead of a local-first skeleton.

5. **Add notifications, resilience, and operational safeguards.**
   Cover crash reporting, notifications, offline recovery, migration-safe database/export flows, and release-oriented hardening for both native apps.

6. **Run QA, UAT, and release cutover.**
    Execute end-to-end parity checks versus legacy, fix regressions, prepare rollout/build signing, and retire the Python/Kivy path only after native replacement is operationally complete.

## Current count
**6 concrete implementation steps remain** after closing the native car-management parity gap, local payroll/table preview-export parity, workbook import staging/parsing in admin, clothes workflow parity, and landing the local driver-account handoff, native vehicle-report PDF export, admin-side settings/report persistence, and the first clothes sizes/orders/items/history/issue/CSV/XLSX export slices.
