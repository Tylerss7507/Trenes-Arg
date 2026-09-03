package com.trenya.app.ui.stationdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.trenya.app.R
import com.trenya.app.data.TrainRepository
import com.trenya.app.data.UserPreferencesRepository
import com.trenya.app.data.model.DataResult
import com.trenya.app.data.model.Station
import com.trenya.app.data.model.UpcomingTrain
import com.trenya.app.ui.LocalAppContainer
import com.trenya.app.ui.components.ArrivalRow
import com.trenya.app.ui.components.EmptyState
import com.trenya.app.ui.components.ErrorState
import com.trenya.app.ui.components.LoadingState
import com.trenya.app.ui.components.OfflineBanner
import com.trenya.app.ui.components.StationLocationMap
import com.trenya.app.ui.theme.TrenYaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val AUTO_REFRESH_MILLIS = 30_000L
private const val TRAINS_TO_FETCH = 12

data class StationDetailUiState(
    val isLoading: Boolean = true,
    val stationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val showMap: Boolean = false,
    val allTrains: List<UpcomingTrain> = emptyList(),
    val branchFilter: Int? = null,
    val isFavorite: Boolean = false,
    val notifyDelays: Boolean = true,
    val errorMessage: String? = null,
    val isShowingCached: Boolean = false
) {
    val visibleTrains: List<UpcomingTrain>
        get() = if (branchFilter == null) allTrains else allTrains.filter { it.branchId == branchFilter }

    val availableBranches: List<Pair<Int, String>>
        get() = allTrains.mapNotNull { t -> t.branchId?.let { it to t.branchName } }.distinct()
}

class StationDetailViewModel(
    private val stationId: String,
    private val trainRepository: TrainRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StationDetailUiState())
    val state: StateFlow<StationDetailUiState> = _state.asStateFlow()

    private var stationRef: Station? = null

    init {
        viewModelScope.launch {
            val station = trainRepository.getStationById(stationId)
            stationRef = station
            _state.value = _state.value.copy(
                stationName = station?.name ?: stationId,
                latitude = station?.latitude,
                longitude = station?.longitude
            )
        }
        viewModelScope.launch {
            userPreferencesRepository.favoritesFlow.collect { favorites ->
                val fav = favorites.firstOrNull { it.stationId == stationId }
                _state.value = _state.value.copy(isFavorite = fav != null, notifyDelays = fav?.notifyDelays ?: true)
            }
        }
        refresh()
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_MILLIS)
                refresh(silent = true)
            }
        }
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            when (val result = trainRepository.getUpcomingTrains(stationId, cantidad = TRAINS_TO_FETCH)) {
                is DataResult.Success -> _state.value = _state.value.copy(
                    isLoading = false,
                    allTrains = result.data,
                    errorMessage = null,
                    isShowingCached = false
                )
                is DataResult.Error -> _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = if (_state.value.allTrains.isEmpty()) result.message else null,
                    isShowingCached = _state.value.allTrains.isNotEmpty()
                )
                DataResult.Loading -> Unit
            }
        }
    }

    fun selectBranch(branchId: Int?) {
        _state.value = _state.value.copy(branchFilter = branchId)
    }

    fun toggleMap() {
        _state.value = _state.value.copy(showMap = !_state.value.showMap)
    }

    fun toggleFavorite() {
        val station = stationRef ?: Station(stationId, _state.value.stationName, 0.0, 0.0, emptyList(), null)
        viewModelScope.launch { userPreferencesRepository.toggleFavorite(station) }
    }

    fun setNotifyDelays(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setNotifyDelays(stationId, enabled) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationDetailScreen(
    stationId: String,
    onBack: () -> Unit,
    onPlanTrip: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: StationDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                StationDetailViewModel(stationId, container.trainRepository, container.userPreferencesRepository)
            }
        }
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.stationName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            if (state.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = stringResource(if (state.isFavorite) R.string.favorites_remove else R.string.favorites_add),
                            tint = if (state.isFavorite) TrenYaColors.Amber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            if (state.isFavorite) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { viewModel.setNotifyDelays(!state.notifyDelays) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (state.notifyDelays) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.station_notify_toggle),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = state.notifyDelays, onCheckedChange = viewModel::setNotifyDelays)
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { onPlanTrip(stationId) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.station_plan_trip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (state.latitude != null && state.longitude != null) {
                val stationLat = state.latitude
                val stationLon = state.longitude
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { viewModel.toggleMap() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(if (state.showMap) R.string.station_hide_map else R.string.station_show_map),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                AnimatedVisibility(visible = state.showMap) {
                    StationLocationMap(
                        latitude = stationLat,
                        longitude = stationLon,
                        stationName = state.stationName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            if (state.availableBranches.size > 1) {
                BranchFilterRow(
                    branches = state.availableBranches,
                    selected = state.branchFilter,
                    onSelect = viewModel::selectBranch
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            when {
                state.isLoading -> LoadingState()
                state.errorMessage != null -> ErrorState(message = state.errorMessage!!, onRetry = { viewModel.refresh() })
                state.visibleTrains.isEmpty() -> EmptyState(title = stringResource(R.string.station_no_trains))
                else -> {
                    Column {
                        if (state.isShowingCached) {
                            OfflineBanner(
                                lastUpdatedLabel = stringResource(R.string.station_offline_notice),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            items(state.visibleTrains, key = { it.serviceNumber + (it.scheduledArrivalIso ?: "") }) { train ->
                                ExpandableArrivalItem(train)
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BranchFilterRow(branches: List<Pair<Int, String>>, selected: Int?, onSelect: (Int?) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.station_filter_all)) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
        )
        branches.forEach { (id, name) ->
            FilterChip(
                selected = selected == id,
                onClick = { onSelect(id) },
                label = { Text(name, maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    }
}

@Composable
private fun ExpandableArrivalItem(train: UpcomingTrain) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.clickable { expanded = !expanded }) {
        ArrivalRow(train)
        AnimatedVisibility(visible = expanded && train.fullRoute.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                Text(
                    stringResource(R.string.station_view_all_stops),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                train.fullRoute.forEach { stop ->
                    Text(
                        stop.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
