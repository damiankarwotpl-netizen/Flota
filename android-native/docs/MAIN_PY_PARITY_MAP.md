# main.py -> Android native parity map

## Source of truth used
- Historical legacy admin application entrypoint: `main.py` from the repository history.
- Screen structure and callbacks referenced by `main.py` imports, especially:
  - `app_modules/home_screen.py`
  - `app_modules/management_screens.py`
  - `app_modules/communication_screens.py`
  - `app_modules/data_screens.py`
  - `app_modules/ui_helpers.py`

## Chosen native environment
`main.py` is a Kivy application with Android permissions, mobile-style navigation, and operational admin flows. The most adequate native target in this repository is therefore **Android mobile**, specifically `android-native/app-admin`.

## Architecture used in the native reconstruction
- `app-admin`: native Android entrypoint and Compose UI shell.
- `core-common`: shared route models, UI state contracts, repository interfaces.
- `core-database`: local persistence, exports, payroll package generation, SMTP / mailing repository logic.
- ViewModels in `app-admin/ui/viewmodel/AdminViewModels.kt`: direct native orchestration of legacy business flows.

## Screen / feature mapping
| Legacy `main.py` element | Native counterpart |
|---|---|
| `setup_home_screen(...)` | `app-admin/ui/screen/HomeScreen.kt` |
| `setup_cars_screen(...)` | `app-admin/ui/screen/CarsScreen.kt` |
| `setup_workers_screen(...)` | `app-admin/ui/screen/WorkersScreen.kt` |
| `setup_plants_screen(...)` | `app-admin/ui/screen/PlantsScreen.kt` |
| `setup_settings_screen(...)` | `app-admin/ui/screen/SettingsScreen.kt` |
| `setup_paski_screen(...)` | `app-admin/ui/screen/PayrollScreen.kt` |
| `setup_email_screen(...)` | `app-admin/ui/screen/EmailScreen.kt` |
| `setup_smtp_screen(...)` | `app-admin/ui/screen/SmtpScreen.kt` |
| `setup_template_screen(...)` | `app-admin/ui/screen/TemplateScreen.kt` |
| `setup_contacts_screen(...)` | `app-admin/ui/screen/ContactsScreen.kt` |
| `setup_report_screen(...)` | `app-admin/ui/screen/ReportsScreen.kt` |
| `setup_table_screen(...)` | `app-admin/ui/screen/TableScreen.kt` |
| `setup_clothes_screen(...)` | `app-admin/ui/screen/ClothesScreen.kt` |
| `setup_vehicle_report_screen(...)` | `app-admin/ui/screen/VehicleReportScreen.kt` |
| `switch_theme("dark"/"light")` | `app-admin/ui/AdminRoot.kt` + `HomeScreen.kt` |
| `start_mass_mailing`, `toggle_pause_mailing`, `start_special_send_flow`, workbook/payroll actions | `PayrollViewModel` + `PayrollScreen.kt` + `EmailScreen.kt` |
| `show_logs_popup(...)` | TODO parity gap — no repository-backed native app-log source yet |

## Feature parity notes
### Home
Legacy home exposed:
- dark / light theme
- navigation to all major modules
- exit action

Native admin now exposes:
- dark / light theme actions on home
- module shortcuts
- exit action

### Payroll / financial logic
Legacy `Paski` screen exposed:
- auto-send toggle
- workbook import
- payroll mailing actions
- export / reports / template / attachments actions
- queue pause/resume

Native admin now centralizes the same operational flow in `PayrollScreen.kt`, backed by `PayrollViewModel` and repository logic.

### Communication
Legacy email flow exposed:
- auto-send toggle
- attachment management
- single file send
- mass send
- pause/resume
- template / SMTP navigation

Native admin preserves this with `EmailScreen.kt` and shared `PayrollViewModel` state.

## Explicit TODO / ambiguity handling
- TODO(main.py parity): `show_logs_popup(...)` in legacy app opens the app log file / in-memory log buffer. The current native codebase does not yet contain an equivalent repository-backed application log source. This should not be guessed or replaced with unrelated session reports.
