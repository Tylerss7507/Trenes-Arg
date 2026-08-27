package com.trenya.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.trenya.app.data.model.ThemeMode
import com.trenya.app.ui.LocalAppContainer
import com.trenya.app.ui.navigation.TrenYaNavHost
import com.trenya.app.ui.onboarding.OnboardingScreen
import com.trenya.app.ui.theme.TrenYaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as TrenYaApplication).container

        setContent {
            val themeMode by container.userPreferencesRepository.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            TrenYaTheme(darkTheme = darkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CompositionLocalProvider(LocalAppContainer provides container) {
                        var onboardingDone by remember { mutableStateOf<Boolean?>(null) }
                        val scope = rememberCoroutineScope()

                        LaunchedEffect(Unit) {
                            onboardingDone = container.userPreferencesRepository.onboardingCompletedFlow.first()
                        }

                        when (onboardingDone) {
                            null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                            false -> OnboardingScreen(
                                onComplete = {
                                    onboardingDone = true
                                    scope.launch { container.userPreferencesRepository.setOnboardingCompleted() }
                                }
                            )
                            true -> TrenYaNavHost()
                        }
                    }
                }
            }
        }
    }
}
