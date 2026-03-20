# Native build failure summary

- Run: https://github.com/damiankarwotpl-netizen/Flota/actions/runs/23361720602
- Workflow: `Build Native Android APKs`
- Build type: `debug`

## Kotlin/Gradle error lines
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/HomeScreen.kt:125:28 Unresolved reference 'TextOverflow'.
w: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/viewmodel/DriverViewModels.kt:216:107 Unchecked cast of 'com.future.ultimate.driver.ui.viewmodel.DriverLoginViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/viewmodel/DriverViewModels.kt:217:111 Unchecked cast of 'com.future.ultimate.driver.ui.viewmodel.DriverMileageViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/viewmodel/DriverViewModels.kt:218:123 Unchecked cast of 'com.future.ultimate.driver.ui.viewmodel.DriverVehicleReportViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/viewmodel/DriverViewModels.kt:219:125 Unchecked cast of 'com.future.ultimate.driver.ui.viewmodel.DriverChangePasswordViewModel' to 'T'.
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app-admin:compileDebugKotlin'.
