plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.trenya.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.trenya.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        // Base de la API pública de arribos. Ver README para más contexto:
        // es un proxy comunitario de la API interna de SOFSE, no un endpoint oficial.
        // Al tenerla como BuildConfig field, cambiarla (por ejemplo a una instancia
        // propia auto-hosteada, o a un endpoint oficial si algún día existe) es
        // cuestión de una sola línea, sin tocar código.
        buildConfigField("String", "API_BASE_URL", "\"https://ariedro.dev/api-trenes/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Red
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // Background work / notificaciones por demoras
    implementation(libs.androidx.work.runtime.ktx)

    // Preferencias y favoritos (sin Room: todo vía DataStore + JSON)
    implementation(libs.androidx.datastore.preferences)

    // Ubicación (estaciones cercanas)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.coroutines.android)

    // Widget de pantalla de inicio (próximo tren favorito)
    implementation(libs.androidx.glance.appwidget)
}
