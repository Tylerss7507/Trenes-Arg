package com.trenya.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.trenya.app.R
import com.trenya.app.data.TrainRepository
import com.trenya.app.data.UserPreferencesRepository
import com.trenya.app.data.model.DataResult
import com.trenya.app.data.model.FavoriteStation
import com.trenya.app.data.model.NearbyStation
import com.trenya.app.data.model.Station
import com.trenya.app.data.model.UpcomingTrain
import com.trenya.app.location.LocationTracker
import com.trenya.app.ui.LocalAppContainer
import com.trenya.app.ui.components.EmptyState
import com.trenya.app.ui.components.LoadingState
import com.trenya.app.ui.components.NextTrainHero
import com.trenya.app.ui.components.SectionHeader
import com.trenya.app.ui.components.StationCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoadingLocation: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val nearbyStations: List<NearbyStation> = emptyList(),
    val favorites: List<FavoriteStation> = emptyList(),
    val heroStationName: String? = null,
    val heroTrain: UpcomingTrain? = null,
    val isLoadingHero: Boolean = false
)

class HomeViewModel(
    private val trainRepository: TrainRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val locationTracker: LocationTracker
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.favoritesFlow.collect { favorites ->
                _state.value = _state.value.copy(favorites = favorites)
                loadHeroTrain(favorites.firstOrNull())
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _state.value = _state.value.copy(hasLocationPermission = granted)
        if (granted) loadNearby()
    }

    fun checkLocationPermission(granted: Boolean) {
        _state.value = _state.value.copy(hasLocationPermission = granted)
        if (granted && _state.value.nearbyStations.isEmpty()) loadNearby()
    }

    fun loadNearby() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingLocation = true)
            val location = locationTracker.getCurrentLocation()
            val nearby = if (location != null) {
                val stations = trainRepository.getAllStations()
                trainRepository.findNearbyStations(location.latitude, location.longitude, stations)
            } else emptyList()
            _state.value = _state.value.copy(nearbyStations = nearby, isLoadingLocation = false)
        }
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch { userPreferencesRepository.toggleFavorite(station) }
    }

    private fun loadHeroTrain(primaryFavorite: FavoriteStation?) {
        if (primaryFavorite == null) {
            _state.value = _state.value.copy(heroTrain = null, heroStationName = null)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingHero = true, heroStationName = primaryFavorite.stationName)
            val result = trainRepository.getUpcomingTrains(primaryFavorite.stationId, cantidad = 1)
            val train = (result as? DataResult.Success)?.data?.firstOrNull()
            _state.value = _state.value.copy(heroTrain = train, isLoadingHero = false)
        }
    }
}

@Composable
fun HomeScreen(onStationClick: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(container.trainRepository, container.userPreferencesRepository, container.locationTracker)
            }
        }
    )
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        viewModel.onLocationPermissionResult(result.values.any { it })
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        viewModel.checkLocationPermission(granted)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                stringResource(R.string.home_greeting),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }

        if (state.heroStationName != null) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    if (state.heroTrain != null) {
                        NextTrainHero(train = state.heroTrain!!, modifier = Modifier.padding(bottom = 8.dp))
                    } else if (state.isLoadingHero) {
                        LoadingState()
                    }
                }
            }
        }

        if (state.favorites.size > 1) {
            item { SectionHeader(stringResource(R.string.home_favorites_title)) }
            items(state.favorites.drop(1), key = { it.stationId }) { fav ->
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    StationCard(
                        station = Station(fav.stationId, fav.stationName, 0.0, 0.0, emptyList(), null),
                        isFavorite = true,
                        onFavoriteToggle = {
                            viewModel.toggleFavorite(Station(fav.stationId, fav.stationName, 0.0, 0.0, emptyList(), null))
                        },
                        onClick = { onStationClick(fav.stationId) }
                    )
                }
            }
        } else if (state.favorites.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.home_favorites_title),
                    subtitle = stringResource(R.string.home_no_favorites),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item { SectionHeader(stringResource(R.string.home_nearby_title)) }

        if (!state.hasLocationPermission) {
            item {
                EmptyState(
                    title = stringResource(R.string.home_nearby_empty),
                    actionLabel = stringResource(R.string.home_nearby_enable_location),
                    onAction = {
                        locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                )
            }
        } else if (state.isLoadingLocation) {
            item { LoadingState() }
        } else if (state.nearbyStations.isEmpty()) {
            item { EmptyState(title = stringResource(R.string.home_nearby_empty)) }
        } else {
            items(state.nearbyStations, key = { it.station.id }) { nearby ->
                val isFav = state.favorites.any { it.stationId == nearby.station.id }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    StationCard(
                        station = nearby.station,
                        distanceMeters = nearby.distanceMeters,
                        isFavorite = isFav,
                        onFavoriteToggle = { viewModel.toggleFavorite(nearby.station) },
                        onClick = { onStationClick(nearby.station.id) }
                    )
                }
            }
        }
    }
}
