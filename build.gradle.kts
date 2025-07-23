// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.agp) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.google.ksp) apply false
    alias(libs.plugins.androidx.safeargs) apply false
    alias(libs.plugins.aboutlibraries) apply false
}

buildscript {
    val isNormalBuild by extra {
        gradle.startParameter.taskNames.none { task ->
            task.contains("fdroid", ignoreCase = true)
        }
    }
    dependencies {
        if (isNormalBuild) {
            classpath(libs.gms.plugin)
            classpath(libs.crashlytics.plugin)
        }
    }
}