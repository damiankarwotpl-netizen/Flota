# Native build failure summary

- Run: https://github.com/damiankarwotpl-netizen/Flota/actions/runs/23451033809
- Workflow: `Build Native Android APKs`
- Build type: `debug`

## Kotlin/Gradle error lines
e: The daemon has terminated unexpectedly on startup attempt #1 with error code: 0. The daemon process output:
e: The daemon has terminated unexpectedly on startup attempt #2 with error code: 0. The daemon process output:
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/CarsScreen.kt:195:45 Too many arguments for 'fun items(strings: List<String>): Unit'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/CarsScreen.kt:195:47 Cannot infer type for this parameter. Please specify it explicitly.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/CarsScreen.kt:197:21 @Composable invocations can only happen from the context of a @Composable function
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app-admin:compileDebugKotlin'.
