# Android native first build guide

This guide explains the first local build of the native Android migration.

## 1. Prerequisites

Before running the build, make sure you have:
- JDK 17 or JDK 21
- Android SDK with platform 35 installed
- network access to Gradle plugin and dependency repositories
- a shell environment with `gradle` available, or a generated Gradle wrapper if your environment uses one

## 2. Set Java

Example with JDK 21:

```bash
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

## 3. Configure Android SDK

Typical setup:

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
```

If your setup relies on `local.properties`, create `android-native/local.properties` with:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

## 4. Go to the Android project

```bash
cd android-native
```

## 5. Build debug APKs first

For the first verification build, create both debug APKs:

```bash
gradle :app-admin:assembleDebug :app-driver:assembleDebug
```

Expected debug artifacts:
- `app-admin/build/outputs/apk/debug/`
- `app-driver/build/outputs/apk/debug/`

## 6. Optional: build signed release artifacts

If you want the first release-ready build, set signing variables first:

```bash
export FLOTA_RELEASE_STORE_FILE=/secure/flota-release.jks
export FLOTA_RELEASE_STORE_PASSWORD='***'
export FLOTA_RELEASE_KEY_ALIAS='flota'
export FLOTA_RELEASE_KEY_PASSWORD='***'
```

Then build release artifacts:

```bash
gradle :app-admin:assembleRelease :app-driver:assembleRelease
```

## 7. What to verify after the first build

1. Both app modules finish without dependency/plugin resolution errors.
2. Debug APK files are present in both output directories.
3. If release signing variables were provided, release artifacts are produced without signing errors.
4. Install the debug build on a test device before starting the QA/UAT checklist.

## 8. Recommended next step

After the first successful build, continue with:
1. `docs/ANDROID_NATIVE_QA_UAT_RELEASE.md`
2. `docs/ANDROID_NATIVE_UAT_SIGNOFF_TEMPLATE.md`

## 9. Common failure modes

### Android plugin cannot be resolved
Make sure the machine has internet access and Gradle can reach Google/Maven Central/Plugin Portal.

### Wrong Java version
If Gradle fails early with a Java compatibility error, switch to JDK 17 or JDK 21 and retry.

### Missing signing credentials
Release builds can stay unsigned for smoke checks, but production rollout requires valid `FLOTA_RELEASE_*` values.
