package com.trenya.app.ui.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.trenya.app.R
import com.trenya.app.data.UserPreferencesRepository
import com.trenya.app.data.model.FavoriteStation
import com.trenya.app.data.model.Station
import com.trenya.app.ui.LocalAppContainer
import com.trenya.app.ui.components.EmptyState
import com.trenya.app.ui.components.StationCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(private val userPreferencesRepository: UserPreferencesRepository) : ViewModel() {

    private val _favorites = MutableStateFlow<List<FavoriteStation>>(emptyList())
    val favorites: StateFlow<List<FavoriteStation>> = _favorites.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.favoritesFlow.collect { _favorites.value = it }
        }
    }

    fun remove(favorite: FavoriteStation) {
        viewModelScope.launch {
            userPreferencesRepository.toggleFavorite(Station(favorite.stationId, favorite.stationName, 0.0, 0.0, emptyList(), null))
        }
    }
}

@Composable
fun FavoritesScreen(onStationClick: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: FavoritesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FavoritesViewModel(container.userPreferencesRepository) }
        }
    )
    val favorites by viewModel.favorites.collectAsState()

    if (favorites.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.favorites_title),
            subtitle = stringResource(R.string.home_no_favorites),
            modifier = Modifier.fillMaxSize().padding(top = 48.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp)
        ) {
            items(favorites, key = { it.stationId }) { favorite ->
                Box(Modifier.padding(vertical = 4.dp)) {
                    StationCard(
                        station = Station(favorite.stationId, favorite.stationName, 0.0, 0.0, emptyList(), null),
                        isFavorite = true,
                        onFavoriteToggle = { viewModel.remove(favorite) },
                        onClick = { onStationClick(favorite.stationId) }
                    )
                }
            }
        }
    }
}
