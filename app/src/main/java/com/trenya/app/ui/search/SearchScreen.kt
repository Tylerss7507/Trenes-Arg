package com.trenya.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
import com.trenya.app.ui.LocalAppContainer
import com.trenya.app.ui.components.EmptyState
import com.trenya.app.ui.components.ErrorState
import com.trenya.app.ui.components.SectionHeader
import com.trenya.app.ui.components.StationCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DEBOUNCE_MILLIS = 300L
private const val MAX_RECENTS = 5

data class SearchUiState(
    val query: String = "",
    val results: List<Station> = emptyList(),
    val isSearching: Boolean = false,
    val recentStations: List<Station> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

class SearchViewModel(
    private val trainRepository: TrainRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            userPreferencesRepository.favoritesFlow.collect { favorites ->
                _state.value = _state.value.copy(favoriteIds = favorites.map { it.stationId }.toSet())
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.value = _state.value.copy(results = emptyList(), isSearching = false, errorMessage = null)
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MILLIS)
            _state.value = _state.value.copy(isSearching = true, errorMessage = null)
            when (val result = trainRepository.searchStations(query)) {
                is DataResult.Success -> _state.value = _state.value.copy(results = result.data, isSearching = false)
                is DataResult.Error -> _state.value = _state.value.copy(results = emptyList(), isSearching = false, errorMessage = result.message)
                DataResult.Loading -> Unit
            }
        }
    }

    fun onStationSelected(station: Station) {
        val updated = (listOf(station) + _state.value.recentStations.filterNot { it.id == station.id }).take(MAX_RECENTS)
        _state.value = _state.value.copy(recentStations = updated)
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch { userPreferencesRepository.toggleFavorite(station) }
    }
}

@Composable
fun SearchScreen(onStationClick: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: SearchViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SearchViewModel(container.trainRepository, container.userPreferencesRepository) }
        }
    )
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = OutlinedTextFieldDefaults.shape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        when {
            state.errorMessage != null -> ErrorState(
                state.errorMessage!!,
                onRetry = { viewModel.onQueryChange(state.query) }
            )
            state.query.isBlank() && state.recentStations.isEmpty() -> {
                EmptyState(title = stringResource(R.string.search_empty_query))
            }
            state.query.isBlank() -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    item { SectionHeader(stringResource(R.string.search_recent)) }
                    items(state.recentStations, key = { it.id }) { station ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            StationCard(
                                station = station,
                                isFavorite = state.favoriteIds.contains(station.id),
                                onFavoriteToggle = { viewModel.toggleFavorite(station) },
                                onClick = {
                                    viewModel.onStationSelected(station)
                                    onStationClick(station.id)
                                }
                            )
                        }
                    }
                }
            }
            state.results.isEmpty() && !state.isSearching -> {
                EmptyState(title = stringResource(R.string.search_no_results))
            }
            else -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(state.results, key = { it.id }) { station ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            StationCard(
                                station = station,
                                isFavorite = state.favoriteIds.contains(station.id),
                                onFavoriteToggle = { viewModel.toggleFavorite(station) },
                                onClick = {
                                    viewModel.onStationSelected(station)
                                    onStationClick(station.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
