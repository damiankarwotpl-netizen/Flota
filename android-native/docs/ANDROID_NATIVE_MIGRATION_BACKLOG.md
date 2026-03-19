# Android native migration backlog

## Remaining steps to full migration

Below is the concrete migration plan used to reach a 1:1 native replacement of the legacy Kivy apps in this repository.

1. **Add real remote driver-account synchronization.**
   Closed in this slice by keeping driver-side session continuity across restarts from the synced local account store and by adding an explicit admin-side endpoint validation path for the production driver contract.

2. **Finish the SMTP pipeline end-to-end.**
   Closed in this slice by adding configurable transport security modes (STARTTLS / SSL/TLS / PLAINTEXT), sender identity, per-message throttling, strict attachment validation, and operator-triggered cancellation for active mailing queues.

3. **Wire the remaining remote API/integration surfaces.**
   Closed in this slice by sharing the native driver remote gateway across admin + driver flows, synchronizing driver-side password resets back to the production API, exposing the remote endpoint in admin settings, and keeping mileage sync on the same production contract.

4. **Add notifications, resilience, and operational safeguards.**
   Closed in this slice by hardening the driver mileage worker with network/battery constraints + exponential backoff, de-duplicated failure notifications, migration-safer timestamped database snapshots with manifest metadata + WAL checkpointing, and manifest-level cleartext-traffic hardening in both apps.

5. **Run QA, UAT, and release cutover.**
   Still open. This slice prepares execution by adding a repository-backed QA/UAT/release playbook, a reusable UAT sign-off template, and environment/property-driven release signing inputs for both Android app modules. The migration is only complete after those checks are run on real target environments and signed release artifacts are approved for rollout.

## Current count
**1 concrete top-level step remains**: execute QA, UAT, signed release validation, and production cutover using the documented playbook and sign-off template.
