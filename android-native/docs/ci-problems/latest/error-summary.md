# Native build failure summary

- Run: https://github.com/damiankarwotpl-netizen/Flota/actions/runs/23335733466
- Workflow: `Build Native Android APKs`
- Build type: `debug`

## Kotlin/Gradle error lines
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:26:39 Unresolved reference 'KeyboardOptions'.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:241:39 Unresolved reference 'KeyboardOptions'.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:382:31 Unresolved reference 'KeyboardOptions'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/AdminRoot.kt:57:53 'val Icons.Outlined.ReceiptLong: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Outlined.ReceiptLong.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:1536:101 Unchecked cast of 'com.future.ultimate.admin.ui.viewmodel.ContactsViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:1537:93 Unchecked cast of 'com.future.ultimate.admin.ui.viewmodel.CarsViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:1538:111 Unchecked cast of 'com.future.ultimate.admin.ui.viewmodel.VehicleReportViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:1539:99 Unchecked cast of 'com.future.ultimate.admin.ui.viewmodel.PayrollViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:1540:95 Unchecked cast of 'com.future.ultimate.admin.ui.viewmodel.TableViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:1541:99 Unchecked cast of 'com.future.ultimate.admin.ui.viewmodel.WorkersViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:1542:97 Unchecked cast of 'com.future.ultimate.admin.ui.viewmodel.PlantsViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:1543:109 Unchecked cast of 'com.future.ultimate.admin.ui.viewmodel.ClothesSizesViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:1544:111 Unchecked cast of 'com.future.ultimate.admin.ui.viewmodel.ClothesOrdersViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:1545:113 Unchecked cast of 'com.future.ultimate.admin.ui.viewmodel.ClothesReportsViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:1546:93 Unchecked cast of 'com.future.ultimate.admin.ui.viewmodel.SmtpViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:1547:101 Unchecked cast of 'com.future.ultimate.admin.ui.viewmodel.TemplateViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:1548:99 Unchecked cast of 'com.future.ultimate.admin.ui.viewmodel.ReportsViewModel' to 'T'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/viewmodel/AdminViewModels.kt:1549:101 Unchecked cast of 'com.future.ultimate.admin.ui.viewmodel.SettingsViewModel' to 'T'.
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app-driver:compileDebugKotlin'.
