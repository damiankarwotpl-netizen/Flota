import org.gradle.api.Project

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val sharedMetaInfResources = setOf(
    "META-INF/NOTICE.md",
    "META-INF/LICENSE.md",
    "META-INF/NOTICE*",
    "META-INF/LICENSE*",
    "/META-INF/NOTICE.md",
    "/META-INF/LICENSE.md",
    "/META-INF/NOTICE*",
    "/META-INF/LICENSE*",
)
val sharedMetaInfPickFirsts = setOf(
    "META-INF/NOTICE.md",
    "META-INF/LICENSE.md",
    "/META-INF/NOTICE.md",
    "/META-INF/LICENSE.md",
)

fun Project.optionalConfig(name: String): String? =
    (findProperty(name) as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.future.ultimate.driver"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.future.ultimate.driver"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            val storePath = project.optionalConfig("FLOTA_RELEASE_STORE_FILE")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = project.optionalConfig("FLOTA_RELEASE_STORE_PASSWORD")
                keyAlias = project.optionalConfig("FLOTA_RELEASE_KEY_ALIAS")
                keyPassword = project.optionalConfig("FLOTA_RELEASE_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += sharedMetaInfResources
            pickFirsts += sharedMetaInfPickFirsts
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core-common"))
    implementation(project(":core-database"))
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
}
