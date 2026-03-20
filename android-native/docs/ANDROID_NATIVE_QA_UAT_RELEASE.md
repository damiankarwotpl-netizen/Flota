# Android native QA / UAT / release cutover

This document supports the final migration workstream by codifying how to verify parity against the legacy Kivy apps, prepare signed Android builds, execute UAT, and cut over production usage to the native applications.

Start with `docs/ANDROID_NATIVE_FIRST_BUILD.md` if you need the exact first-build sequence.

## 0. Repository gate (recommended before QA/UAT)

Run the cutover gate script to capture an auditable preflight report in
`docs/NATIVE_ANDROID_MIGRATION_EXECUTION_LOG.md`:

```bash
./scripts/run_cutover_gate.sh
```

The script checks Java/Gradle prerequisites, attempts both debug builds, and records
whether release-signing inputs are already present. If the gate fails, resolve the
reported blockers before moving into device QA/UAT.

## 1. Release prerequisites

### Toolchain
- JDK 17 or JDK 21
- Android SDK / platform 35
- Gradle able to resolve the Android plugins declared in `android-native/build.gradle.kts`

### Release signing inputs
Both Android app modules accept release-signing credentials either as Gradle properties or environment variables:
- `FLOTA_RELEASE_STORE_FILE`
- `FLOTA_RELEASE_STORE_PASSWORD`
- `FLOTA_RELEASE_KEY_ALIAS`
- `FLOTA_RELEASE_KEY_PASSWORD`

If these values are omitted, `release` builds stay unsigned, which is acceptable for local smoke checks but not for production rollout.

Example:

```bash
export JAVA_HOME=/path/to/jdk-21
export FLOTA_RELEASE_STORE_FILE=/secure/flota-release.jks
export FLOTA_RELEASE_STORE_PASSWORD='***'
export FLOTA_RELEASE_KEY_ALIAS='flota'
export FLOTA_RELEASE_KEY_PASSWORD='***'
cd android-native
gradle :app-admin:assembleRelease :app-driver:assembleRelease
```

## 2. QA parity matrix

### Admin app
Run these flows against the native app and confirm parity with the legacy Kivy behavior:
1. Cars CRUD, assignment, driver password reset, and remote sync status visibility.
2. Workers, payroll calculations, workbook import staging, and payroll exports.
3. Email flows: SMTP validation, single send, mass send, pause/resume, cancellation, operator approval, special sends, session report export, and attachment validation.
4. Clothes sizes, orders, order items/history, issue workflow, and CSV/XLSX/PDF exports.
5. Settings persistence, endpoint validation, report retention, and backup/export safeguards.

### Driver app
1. Login with synced local account, session restoration after process death, and password reset propagation.
2. Mileage entry queueing, background sync, retry handling, and notification delivery.
3. Vehicle report creation/export and offline-to-online reconciliation.

### Regression gates
Before sign-off, confirm:
- no cleartext-traffic regressions
- no notification spam during repeated sync failures
- no data loss after app restart or background execution
- no missing local export artifacts required by admin mail workflows

## 3. UAT sign-off checklist

Capture explicit sign-off for each area below:
- Fleet/admin operations owner
- Driver operations owner
- SMTP / email operations owner
- Release owner responsible for keystore + store upload

Use `docs/ANDROID_NATIVE_UAT_SIGNOFF_TEMPLATE.md` to capture the formal sign-off packet for each release candidate.

## 4. Cutover plan

1. Freeze legacy data edits long enough to validate the latest native database snapshot/export.
2. Produce signed `release` builds for both Android apps.
3. Install the builds on the pilot admin + driver devices.
4. Execute the QA parity matrix above.
5. Execute UAT with production-like SMTP and remote driver endpoint settings.
6. Export/retain the last pre-cutover backup from the legacy path.
7. Announce the cutover window.
8. Switch operators to the native admin app and drivers to the native driver app.
9. Monitor first-day mileage sync, SMTP sessions, notifications, and remote driver-account operations.

## 5. Rollback plan

Rollback if any blocking regression appears in mail delivery, mileage synchronization, driver login, or data integrity:
1. Stop new native data entry.
2. Preserve the native database snapshot/export for forensics.
3. Re-enable the legacy Kivy operational path.
4. Reconcile any records created during the failed cutover window.
5. Fix the blocking issue and rerun the QA/UAT checklist before retrying cutover.

## 6. Completion rule

The Android migration is considered operationally complete only after all of the following happen in a real target environment:
- the QA parity matrix passes
- UAT sign-off is recorded in `docs/ANDROID_NATIVE_UAT_SIGNOFF_TEMPLATE.md`
- signed release artifacts are produced
- production operators complete cutover without rollback
