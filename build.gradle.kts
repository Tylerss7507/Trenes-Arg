// Build script raíz. Los módulos (app) aplican estos plugins con "apply false"
// para que las versiones queden centralizadas acá y en gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
