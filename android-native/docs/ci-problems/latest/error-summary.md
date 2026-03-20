# Native build failure summary

- Run: https://github.com/damiankarwotpl-netizen/Flota/actions/runs/23335177222
- Workflow: `Build Native Android APKs`
- Build type: `debug`

## Kotlin/Gradle error lines
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:26:39 Unresolved reference 'KeyboardOptions'.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:241:39 Unresolved reference 'KeyboardOptions'.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:382:31 Unresolved reference 'KeyboardOptions'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/SettingsScreen.kt:33:69 Unresolved reference 'dp'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/SettingsScreen.kt:46:70 Unresolved reference 'dp'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/SettingsScreen.kt:68:70 Unresolved reference 'dp'.
FAILURE: Build completed with 2 failures.
* What went wrong:
Execution failed for task ':app-driver:compileDebugKotlin'.
* What went wrong:
Execution failed for task ':app-admin:compileDebugKotlin'.
