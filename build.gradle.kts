plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
}

// Move build directories outside of OneDrive to prevent file-lock conflicts during sync
allprojects {
    val buildPath = if (project.path == ":") "root"
                    else project.path.replace(":", "/").removePrefix("/")
    layout.buildDirectory.set(
        File(System.getProperty("user.home"), ".gradle-builds/bolao-copa-2026/$buildPath")
    )
}
