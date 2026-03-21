# Native build failure summary

- Run: https://github.com/damiankarwotpl-netizen/Flota/actions/runs/23378772351
- Workflow: `Build Native Android APKs`
- Build type: `debug`

## Kotlin/Gradle error lines
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:117:9 Unresolved reference 'AlertDialog'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:119:23 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:121:17 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:159:17 @Composable invocations can only happen from the context of a @Composable function
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app-admin:compileDebugKotlin'.
