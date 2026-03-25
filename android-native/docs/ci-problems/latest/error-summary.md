# Native build failure summary

- Run: https://github.com/damiankarwotpl-netizen/Flota/actions/runs/23539549567
- Workflow: `Build Native Android APKs`
- Build type: `debug`

## Kotlin/Gradle error lines
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:478:92 Unresolved reference 'ReportField'.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:534:32 Unresolved reference 'ReportField'.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:535:13 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:536:31 Unresolved reference 'value'.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:539:37 Unresolved reference 'id'.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:559:25 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:559:34 Unresolved reference 'labelPl'.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:559:49 Unresolved reference 'labelEs'.
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:560:38 Unresolved reference 'keyboardType'.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/AdminMenu.kt:28:53 'val Icons.Rounded.ReceiptLong: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Rounded.ReceiptLong.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/ContactsScreen.kt:210:49 'val Icons.Rounded.Chat: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Rounded.Chat.
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app-driver:compileDebugKotlin'.
