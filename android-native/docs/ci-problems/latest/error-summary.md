# Native build failure summary

- Run: https://github.com/damiankarwotpl-netizen/Flota/actions/runs/23378127174
- Workflow: `Build Native Android APKs`
- Build type: `debug`

## Kotlin/Gradle error lines
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:5:42 Conflicting import: imported name 'ActivityResultContracts' is ambiguous.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:11:43 Cannot access 'val RowColumnParentData?.weight: Float': it is internal in file.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:19:42 Conflicting import: imported name 'ActivityResultContracts' is ambiguous.
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app-admin:compileDebugKotlin'.
