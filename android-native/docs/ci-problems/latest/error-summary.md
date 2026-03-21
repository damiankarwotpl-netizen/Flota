# Native build failure summary

- Run: https://github.com/damiankarwotpl-netizen/Flota/actions/runs/23380950119
- Workflow: `Build Native Android APKs`
- Build type: `debug`

## Kotlin/Gradle error lines
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:172:51 Unresolved reference 'PayrollPreviewRow'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:206:28 Unresolved reference 'com.future.ultimate.core.common.ui.PayrollPreviewRow'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:207:17 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:216:43 Unresolved reference 'index'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:217:65 Unresolved reference 'index'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:222:40 Unresolved reference 'cells'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:235:62 Unresolved reference 'index'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:239:42 Unresolved reference 'isPreviewDialogOpen'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:243:17 Unresolved reference 'uiState'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:243:40 Cannot infer type for this parameter. Please specify it explicitly.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:243:44 Cannot infer type for this parameter. Please specify it explicitly.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:269:12 Null cannot be a value of a non-null type 'kotlin.Unit'.
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app-admin:compileDebugKotlin'.
