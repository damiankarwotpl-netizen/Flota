# Android native migration backlog

## Remaining steps to full migration

Below is the concrete **10-step plan** to reach a 1:1 native replacement of the legacy Kivy apps in this repository.

1. **Close the remaining car-management gaps.**
   Finish native car CRUD parity around service lifecycle, richer search/actions, and edge cases still present in the legacy `cars` / `fleet_cars` flows.

2. **Add real remote driver-account synchronization.**
   Replace the current local-only handoff with the production API contract for driver credentials, reset flows, and car-to-driver assignment sync.

3. **Implement mileage/background sync for the driver app.**
   Add periodic native background work for mileage refresh, offline queueing, retry handling, and admin-side visibility into sync freshness.

4. **Port workbook import into the native admin flow.**
   Migrate the legacy `paski` workbook ingest path, validation, parsing, and local staging so the Android admin app can prepare mailing/export sessions without the Python stack.

5. **Finish payroll/table preview/export parity.**
   Bring over table preview, row filtering, per-row export, package preparation, and the remaining payroll-import/export actions from the legacy `paski` + `table` screens.

6. **Finish the SMTP pipeline end-to-end.**
   Move from local config/template persistence to real SMTP send, test connection, attachments, special-send flow, and mass mailing queue behavior now still living in the Kivy app.

7. **Complete the clothes workflow to 1:1 parity.**
   Add the remaining order/report polish from legacy clothes flows: import from Excel, PDF exports, partial issue UX, and any missing detail actions around history/order handling.

8. **Wire the remaining remote API/integration surfaces.**
   Port the missing production integrations used by admin and driver flows so native Android becomes the primary runtime instead of a local-first skeleton.

9. **Add notifications, resilience, and operational safeguards.**
   Cover crash reporting, notifications, offline recovery, migration-safe database/export flows, and release-oriented hardening for both native apps.

10. **Run QA, UAT, and release cutover.**
    Execute end-to-end parity checks versus legacy, fix regressions, prepare rollout/build signing, and retire the Python/Kivy path only after native replacement is operationally complete.

## Current count
**10 concrete implementation steps remain** after landing the local driver-account handoff, native vehicle-report PDF export, admin-side settings/report persistence, and the first clothes sizes/orders/items/history/issue/CSV/XLSX export slices.
