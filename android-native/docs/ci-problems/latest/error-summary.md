# Native build failure summary

- Run: https://github.com/damiankarwotpl-netizen/Flota/actions/runs/23565159182
- Workflow: `Build Native Android APKs`
- Build type: `debug`

## Kotlin/Gradle error lines
e: file:///home/runner/work/Flota/Flota/android-native/app-driver/src/main/java/com/future/ultimate/driver/ui/screen/DriverScreens.kt:596:53 @Composable invocations can only happen from the context of a @Composable function
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/AdminMenu.kt:28:53 'val Icons.Rounded.ReceiptLong: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Rounded.ReceiptLong.
w: file:///home/runner/work/Flota/Flota/android-native/app-admin/src/main/java/com/future/ultimate/admin/ui/screen/ContactsScreen.kt:210:49 'val Icons.Rounded.Chat: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Rounded.Chat.
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app-driver:compileDebugKotlin'.
