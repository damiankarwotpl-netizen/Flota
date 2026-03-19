# Android native migration backlog

## Remaining steps to full migration

Below is the concrete **5-step plan** to reach a 1:1 native replacement of the legacy Kivy apps in this repository.

1. **Add real remote driver-account synchronization.**
   The native admin now pushes remote create/reset/delete + car-assignment sync requests for driver accounts and exposes sync status per vehicle. Remaining work is driver-side remote login/parity hardening and final production contract validation.

2. **Finish the SMTP pipeline end-to-end.**
   The native app now covers real SMTP send, test connection, attachments, special-send flow, pausable mass-mailing queue behavior, and operator-review gating when auto-send is disabled. Remaining work is production delivery edge cases and final rollout validation.

3. **Wire the remaining remote API/integration surfaces.**
   Port the missing production integrations used by admin and driver flows so native Android becomes the primary runtime instead of a local-first skeleton.

4. **Add notifications, resilience, and operational safeguards.**
   The first notification slice is now in place for driver mileage background sync. Remaining work covers broader crash reporting, offline recovery, migration-safe database/export flows, and release-oriented hardening for both native apps.

5. **Run QA, UAT, and release cutover.**
    Execute end-to-end parity checks versus legacy, fix regressions, prepare rollout/build signing, and retire the Python/Kivy path only after native replacement is operationally complete.

## Current count
**5 concrete implementation steps remain** after closing the native car-management parity gap, local payroll/table preview-export parity, workbook import staging/parsing in admin, clothes workflow parity, landing the local driver-account handoff, native vehicle-report PDF export, admin-side settings/report persistence, the first clothes sizes/orders/items/history/issue/CSV/XLSX export slices, the driver mileage background/queue sync slice with admin-side freshness visibility, the native SMTP special-send + pausable mailing-queue + operator-review slice, the first remote driver-account sync/status slice, and now also the first driver notification/hardening slice.
