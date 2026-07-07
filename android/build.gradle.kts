// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

allprojects {
    val safeProjectName = path.replace(':', '_').ifBlank { "root" }
    val localBuildRoot = System.getenv("LOCALAPPDATA")
        ?.let { java.io.File(it, "AstraMeshGradleBuild") }
        ?: rootProject.layout.projectDirectory.dir(".gradle-build").asFile

    layout.buildDirectory.set(java.io.File(localBuildRoot, safeProjectName))
}
