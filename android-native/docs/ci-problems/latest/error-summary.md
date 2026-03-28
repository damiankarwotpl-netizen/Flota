# Native build failure summary

- Run: https://github.com/damiankarwotpl-netizen/Flota/actions/runs/23681033717
- Workflow: `Build Native Android APKs`
- Build type: `debug`

## Kotlin/Gradle error lines
e: The daemon has terminated unexpectedly on startup attempt #1 with error code: 0. The daemon process output:
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/ClothesScreen.kt:372:17 Unresolved reference 'item'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/ClothesScreen.kt:373:21 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/ClothesScreen.kt:388:17 Unresolved reference 'item'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/ClothesScreen.kt:389:21 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/ClothesScreen.kt:396:1 Syntax error: Expecting a top level declaration.
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app-admin:compileDebugKotlin'.
