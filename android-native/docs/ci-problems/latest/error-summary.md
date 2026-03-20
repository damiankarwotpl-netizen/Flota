# Native build failure summary

- Run: https://github.com/damiankarwotpl-netizen/Flota/actions/runs/23316461277
- Workflow: `Build Native Android APKs`
- Build type: `debug`

## Kotlin/Gradle error lines
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/DriverApp.kt:15:91 Cannot access 'androidx.room.RoomDatabase' which is a supertype of 'com.future.ultimate.core.database.FlotaDatabase'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/sync/DriverMileageSyncWorker.kt:22:62 Cannot access 'androidx.room.RoomDatabase' which is a supertype of 'com.future.ultimate.core.database.FlotaDatabase'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:177:21 Argument type mismatch: actual type is 'kotlin.Any', but 'com.future.ultimate.core.common.model.VehicleReportDraft' was expected.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:187:25 'else' entry must be the last one in a 'when' expression.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:194:26 Syntax error: Expecting a when-condition.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:195:21 Syntax error: Expecting an expression, is-condition or in-condition.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/AdminApp.kt:15:52 Cannot access 'androidx.room.RoomDatabase' which is a supertype of 'com.future.ultimate.core.database.FlotaDatabase'. Check your module classpath for missing or conflicting dependencies.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/ClothesScreen.kt:8:43 Cannot access 'val RowColumnParentData?.weight: Float': it is internal in file.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/Common.kt:41:15 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/EmailScreen.kt:7:43 Cannot access 'val RowColumnParentData?.weight: Float': it is internal in file.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:111:22 Unresolved reference 'CarListItem'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:114:26 Unresolved reference 'id'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:115:28 Unresolved reference 'name'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:116:36 Unresolved reference 'registration'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:117:30 Unresolved reference 'driver'.
e: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:118:39 Unresolved reference 'serviceInterval'.
FAILURE: Build completed with 2 failures.
* What went wrong:
Execution failed for task ':app-driver:compileDebugKotlin'.
* What went wrong:
Execution failed for task ':app-admin:compileDebugKotlin'.
