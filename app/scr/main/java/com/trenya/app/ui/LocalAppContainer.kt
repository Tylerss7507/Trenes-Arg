package com.trenya.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.trenya.app.core.AppContainer

/**
 * Da acceso al [AppContainer] (repos, ubicación, notificaciones) desde
 * cualquier composable, para que cada pantalla arme su propio ViewModel con
 * `viewModel(factory = viewModelFactory { initializer { ... } })` sin pasar
 * las dependencias a mano por cada nivel de la navegación.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer no fue provisto. Envolvé la app con CompositionLocalProvider en MainActivity.")
}
