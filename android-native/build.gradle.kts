import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project

plugins {
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}

val sharedMetaInfResources = setOf(
    "META-INF/NOTICE.md",
    "META-INF/LICENSE.md",
    "/META-INF/NOTICE.md",
    "/META-INF/LICENSE.md",
)

fun Project.configureSharedPackagingResources() {
    pluginManager.withPlugin("com.android.application") {
        extensions.configure(ApplicationExtension::class.java) {
            packaging.resources.excludes.addAll(sharedMetaInfResources)
            packaging.resources.pickFirsts.addAll(sharedMetaInfResources)
        }
    }

    pluginManager.withPlugin("com.android.library") {
        extensions.configure(LibraryExtension::class.java) {
            packaging.resources.excludes.addAll(sharedMetaInfResources)
            packaging.resources.pickFirsts.addAll(sharedMetaInfResources)
        }
    }
}

subprojects {
    configureSharedPackagingResources()
}
