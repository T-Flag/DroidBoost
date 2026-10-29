plugins {
    alias(libs.plugins.android.application) apply false
    // Not applied (AGP 9 compiles Kotlin itself): only pins the Kotlin Gradle plugin version AGP uses.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
