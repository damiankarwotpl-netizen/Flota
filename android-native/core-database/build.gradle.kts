plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.future.ultimate.core.database"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = false
    }

    packaging {
        resources {
            val duplicateMetaInf = setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "/META-INF/NOTICE.md",
                "/META-INF/LICENSE.md",
            )
            excludes += duplicateMetaInf
            pickFirsts += duplicateMetaInf
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
    api(project(":core-common"))
    implementation("androidx.core:core-ktx:1.15.0")
    api("androidx.room:room-runtime:2.6.1")
    api("androidx.room:room-ktx:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    compileOnly("com.sun.mail:android-mail:1.6.7")
    compileOnly("com.sun.mail:android-activation:1.6.7")
    ksp("androidx.room:room-compiler:2.6.1")
}
