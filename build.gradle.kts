// All plugins are declared here, so they share one class loader. The Compose
// plugin also brings the Kotlin version that Android's built-in Kotlin uses.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
