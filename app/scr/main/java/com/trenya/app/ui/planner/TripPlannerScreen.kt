package com.trenya.app.ui.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.trenya.app.data.model.DataResult
import com.trenya.app.data.model.Station
import com.trenya.app.data.model.UpcomingTrain
import com.trenya.app.ui.LocalAppContainer
import com.trenya.app.ui.components.ArrivalRow
import com.trenya.app.ui.components.EmptyState
import com.trenya.app.ui.components.ErrorState
import com.trenya.app.ui.components.LoadingState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

private const val DEBOUNCE_MILLIS = 300L

data class TripPlannerUiState(
    val originQuery: String = "",
    val destinationQuery: String = "",
    val selectedOrigin: Station? = null,
    val selectedDestination: Station? = null,
    val originResults: List<Station> = emptyList(),
    val destinationResults: List<Station> = emptyList(),
    val trains: List<UpcomingTrain> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null
)

class TripPlannerViewModel(private val trainRepository: TrainRepository) : ViewModel() {

    private val _state = MutableStateFlow(TripPlannerUiState())
    val state: StateFlow<TripPlannerUiState> = _state.asStateFlow()

    private var originJob: Job? = null
    private var destinationJob: Job? = null

    fun presetOrigin(station: Station) {
        _state.value = _state.value.copy(selectedOrigin = station, originQuery = station.name, originResults = emptyList())
    }

    fun setOriginQuery(query: String) {
        _state.value = _state.value.copy(originQuery = query, selectedOrigin = null)
        originJob?.cancel()
        if (query.isBlank()) {
            _state.value = _state.value.copy(originResults = emptyList())
            return
        }
        originJob = viewModelScope.launch {
            delay(DEBOUNCE_MILLIS)
            _state.value = _state.value.copy(originResults = trainRepository.searchStations(query))
        }
    }

    fun setDestinationQuery(query: String) {
        _state.value = _state.value.copy(destinationQuery = query, selectedDestination = null)
        destinationJob?.cancel()
        if (query.isBlank()) {
            _state.value = _state.value.copy(destinationResults = emptyList())
            return
        }
        destinationJob = viewModelScope.launch {
            delay(DEBOUNCE_MILLIS)
            _state.value = _state.value.copy(destinationResults = trainRepository.searchStations(query))
        }
    }

    fun selectOrigin(station: Station) {
        _state.value = _state.value.copy(selectedOrigin = station, originQuery = station.name, originResults = emptyList())
    }

    fun selectDestination(station: Station) {
        _state.value = _state.value.copy(selectedDestination = station, destinationQuery = station.name, destinationResults = emptyList())
    }

    fun swap() {
        val s = _state.value
        _state.value = s.copy(
            selectedOrigin = s.selectedDestination,
            selectedDestination = s.selectedOrigin,
            originQuery = s.destinationQuery,
            destinationQuery = s.originQuery
        )
    }

    fun search() {
        val origin = _state.value.selectedOrigin ?: return
        val destination = _state.value.selectedDestination ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, hasSearched = true, errorMessage = null)
            when (val result = trainRepository.getUpcomingTrains(origin.id, destinationStationId = destination.id, cantidad = 6)) {
                is DataResult.Success -> _state.value = _state.value.copy(isLoading = false, trains = result.data)
                is DataResult.Error -> _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                DataResult.Loading -> Unit
            }
        }
    }

    /** Duración estimada del tramo origen→destino, si el recorrido completo del servicio la trae. */
    fun estimateDurationMinutes(train: UpcomingTrain): Long? {
        val destinationId = _state.value.selectedDestination?.id ?: return null
        val destinationStop = train.fullRoute.firstOrNull { it.stationId?.toString() == destinationId } ?: return null
        val arrival = destinationStop.scheduledArrival ?: return null
        val departure = train.scheduledArrivalIso ?: return null
        return runCatching {
            Duration.between(Instant.parse(departure), Instant.parse(arrival)).toMinutes().takeIf { it > 0 }
        }.getOrNull()
    }
}

@Composable
fun TripPlannerScreen(presetOriginId: String? = null) {
    val container = LocalAppContainer.current
    val viewModel: TripPlannerViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TripPlannerViewModel(container.trainRepository) }
        }
    )
    val state by viewModel.state.collectAsState()

    LaunchedEffect(presetOriginId) {
        if (presetOriginId != null) {
            container.trainRepository.getStationById(presetOriginId)?.let { viewModel.presetOrigin(it) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.planner_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                StationField(
                    label = stringResource(R.string.planner_origin),
                    query = state.originQuery,
                    onQueryChange = viewModel::setOriginQuery,
                    results = state.originResults,
                    onSelect = viewModel::selectOrigin
                )
                Spacer(Modifier.height(8.dp))
                StationField(
                    label = stringResource(R.string.planner_destination),
                    query = state.destinationQuery,
                    onQueryChange = viewModel::setDestinationQuery,
                    results = state.destinationResults,
                    onSelect = viewModel::selectDestination
                )
            }
            IconButton(onClick = viewModel::swap) {
                Icon(Icons.Filled.SwapVert, contentDescription = stringResource(R.string.planner_swap))
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = viewModel::search,
            enabled = state.selectedOrigin != null && state.selectedDestination != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.planner_search))
        }

        Spacer(Modifier.height(12.dp))

        when {
            !state.hasSearched -> Unit
            state.isLoading -> LoadingState()
            state.errorMessage != null -> ErrorState(state.errorMessage!!, onRetry = viewModel::search)
            state.trains.isEmpty() -> EmptyState(title = stringResource(R.string.planner_no_direct))
            else -> {
                LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(state.trains, key = { it.serviceNumber }) { train ->
                        Column {
                            ArrivalRow(train)
                            viewModel.estimateDurationMinutes(train)?.let { minutes ->
                                Text(
                                    stringResource(
                                        R.string.planner_duration,
                                        if (minutes < 60) "$minutes min" else "${minutes / 60}h ${minutes % 60}min"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StationField(
    label: String,
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Station>,
    onSelect: (Station) -> Unit
) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(label) },
            singleLine = true,
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = null)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (results.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                results.take(4).forEach { station ->
                    Text(
                        station.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(station) }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}
