# Native Android migration execution log

## Latest run
- Timestamp (UTC): 2026-03-19T18:47:14Z
- Overall gate: FAIL

## Gate checks
| Check | Status | Details |
| --- | --- | --- |
| Java toolchain (17/21) | PASS | JAVA_HOME=/root/.local/share/mise/installs/java/21.0.2 (major=21) |
| Gradle availability | PASS | Using gradle |
| Debug build (`:app-admin:assembleDebug :app-driver:assembleDebug`) | FAIL | Debug build failed (exit=1): * Try: > Run with --stacktrace option to get the stack trace. > Run with --info or --debug option to get more log output. > Run with --scan to get full insights. > Get more help at https://help.gradle.org. BUILD FAILED in 1s |
| Release signing inputs (`FLOTA_RELEASE_*`) | WARN | Release signing vars are missing (expected in production cutover) |

## Completion interpretation
- Migration can be marked operationally complete only after QA/UAT + signed release cutover are executed and approved in real target environments.
- If this file reports a failing gate, treat migration as not yet operationally complete.
