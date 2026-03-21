# Native build failure summary

- Run: https://github.com/damiankarwotpl-netizen/Flota/actions/runs/23381350597
- Workflow: `Build Native Android APKs`
- Build type: `debug`

## Kotlin/Gradle error lines
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:55:24 Overload resolution ambiguity between candidates:
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:195:51 Unresolved reference 'PayrollPreviewRow'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:229:28 Unresolved reference 'com.future.ultimate.core.common.ui.PayrollPreviewRow'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:230:17 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:239:43 Unresolved reference 'index'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:240:65 Unresolved reference 'index'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:245:40 Unresolved reference 'cells'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:258:62 Unresolved reference 'index'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:262:42 Unresolved reference 'isPreviewDialogOpen'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:266:17 Unresolved reference 'uiState'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:266:40 Cannot infer type for this parameter. Please specify it explicitly.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:266:44 Cannot infer type for this parameter. Please specify it explicitly.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:292:12 Null cannot be a value of a non-null type 'kotlin.Unit'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:295:1 Conflicting overloads:
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:306:1 Conflicting overloads:
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/PayrollScreen.kt:317:1 Conflicting overloads:
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app-admin:compileDebugKotlin'.
