package com.trenya.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.trenya.app.R
import com.trenya.app.core.DistanceUtils
import com.trenya.app.data.model.Station
import com.trenya.app.data.model.TrainStatus
import com.trenya.app.data.model.UpcomingTrain
import com.trenya.app.ui.theme.CountdownTextStyle
import com.trenya.app.ui.theme.TrenYaColors

@Composable
fun formatCountdown(secondsRemaining: Long?): String {
    if (secondsRemaining == null) return stringResource(R.string.status_unknown)
    if (secondsRemaining <= 30) return stringResource(R.string.station_arrived)
    val totalMinutes = secondsRemaining / 60
    return if (totalMinutes < 60) {
        stringResource(R.string.minutes_short, totalMinutes.toInt())
    } else {
        stringResource(R.string.hours_minutes_short, (totalMinutes / 60).toInt(), (totalMinutes % 60).toInt())
    }
}

private fun statusColor(status: TrainStatus): Color = when (status) {
    TrainStatus.NORMAL -> TrenYaColors.OnTime
    TrainStatus.DELAYED, TrainStatus.ALTERED -> TrenYaColors.Amber
    TrainStatus.CANCELLED -> TrenYaColors.Delayed
    TrainStatus.UNKNOWN -> Color.Gray
}

private fun statusLabelRes(status: TrainStatus): Int = when (status) {
    TrainStatus.NORMAL -> R.string.status_normal
    TrainStatus.DELAYED -> R.string.status_delayed
    TrainStatus.ALTERED -> R.string.status_altered
    TrainStatus.CANCELLED -> R.string.status_cancelled
    TrainStatus.UNKNOWN -> R.string.status_unknown
}

@Composable
fun LineChip(lineName: String, modifier: Modifier = Modifier) {
    val color = TrenYaColors.forLine(lineName)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(lineName, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatusBadge(status: TrainStatus, modifier: Modifier = Modifier) {
    if (status == TrainStatus.NORMAL) return
    val color = statusColor(status)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(statusLabelRes(status)),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StationCard(
    station: Station,
    distanceMeters: Double? = null,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Train,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(station.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                if (distanceMeters != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            DistanceUtils.formatDistance(distanceMeters),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (onFavoriteToggle != null) {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = stringResource(if (isFavorite) R.string.favorites_remove else R.string.favorites_add),
                        tint = if (isFavorite) TrenYaColors.Amber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ArrivalRow(train: UpcomingTrain, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.station_towards, train.destinationName),
                style = MaterialTheme.typography.titleMedium,
                textDecoration = if (train.status == TrainStatus.CANCELLED) TextDecoration.LineThrough else null,
                maxLines = 1
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LineChip(train.lineName)
                StatusBadge(train.status)
            }
            if (train.status != TrainStatus.NORMAL && !train.statusMessage.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    train.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor(train.status)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatCountdown(train.secondsRemaining),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (train.status == TrainStatus.NORMAL) MaterialTheme.colorScheme.onSurface else statusColor(train.status)
            )
            if (!train.platform.isNullOrBlank()) {
                Text(
                    stringResource(R.string.station_platform, train.platform),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NextTrainHero(train: UpcomingTrain, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LineChipOnColor(train.lineName)
                if (train.status != TrainStatus.NORMAL) {
                    Text(
                        stringResource(statusLabelRes(train.status)),
                        style = MaterialTheme.typography.labelMedium,
                        color = TrenYaColors.Amber,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            AnimatedContent(
                targetState = formatCountdown(train.secondsRemaining),
                label = "countdown",
                transitionSpec = { fadeThroughSpec() }
            ) { text ->
                Text(text, style = CountdownTextStyle, color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.station_towards, train.destinationName),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )
            if (!train.platform.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.station_platform, train.platform),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
            }
        }
    }
}

private fun fadeThroughSpec() =
    (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) +
        androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(220), initialScale = 0.92f))
        .togetherWith(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)))

@Composable
private fun LineChipOnColor(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    EmptyState(
        title = message,
        actionLabel = onRetry?.let { stringResource(R.string.retry) },
        onAction = onRetry,
        modifier = modifier
    )
}

@Composable
fun OfflineBanner(lastUpdatedLabel: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(lastUpdatedLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun StationLocationMap(
    latitude: Double,
    longitude: Double,
    stationName: String,
    modifier: Modifier = Modifier
) {
    val stationPosition = remember(latitude, longitude) { LatLng(latitude, longitude) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(stationPosition, 15f)
    }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(position = stationPosition),
            title = stationName
        )
    }
}
