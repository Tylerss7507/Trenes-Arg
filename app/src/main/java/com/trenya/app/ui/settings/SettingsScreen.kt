package com.trenya.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.content.Context
import com.trenya.app.BuildConfig
import com.trenya.app.R
import com.trenya.app.core.Constants
import com.trenya.app.data.UserPreferencesRepository
import com.trenya.app.data.model.PollInterval
import com.trenya.app.data.model.ThemeMode
import com.trenya.app.notification.DelayCheckWorker
import com.trenya.app.ui.LocalAppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val pollInterval: PollInterval = PollInterval.THIRTY,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

class SettingsViewModel(
    private val repo: UserPreferencesRepository,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.notificationsEnabledFlow.collect { _state.value = _state.value.copy(notificationsEnabled = it) }
        }
        viewModelScope.launch {
            repo.pollIntervalFlow.collect { _state.value = _state.value.copy(pollInterval = it) }
        }
        viewModelScope.launch {
            repo.themeModeFlow.collect { _state.value = _state.value.copy(themeMode = it) }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { repo.setNotificationsEnabled(enabled) }

    fun setPollInterval(interval: PollInterval) = viewModelScope.launch {
        repo.setPollInterval(interval)
        DelayCheckWorker.schedule(appContext, interval.minutes)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
}

@Composable
fun SettingsScreen() {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(container.userPreferencesRepository, context.applicationContext) } }
    )
    val state by viewModel.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_notifications), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.settings_notifications_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = state.notificationsEnabled, onCheckedChange = viewModel::setNotificationsEnabled)
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.settings_poll_interval), style = MaterialTheme.typography.titleMedium)
        PollInterval.entries.forEach { interval ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = state.pollInterval == interval, onClick = { viewModel.setPollInterval(interval) })
                Text(interval.label, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
        val themeOptions = listOf(
            ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
            ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
            ThemeMode.DARK to stringResource(R.string.settings_theme_dark)
        )
        themeOptions.forEach { (mode, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = state.themeMode == mode, onClick = { viewModel.setThemeMode(mode) })
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.settings_about_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.settings_data_source) + ": ${Constants.LINK_API_SOURCE}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}
