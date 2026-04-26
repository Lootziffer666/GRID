// Top-level build file. Plugin versions are declared in gradle/libs.versions.toml
// and applied in module build files. Keep this file minimal on purpose.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
