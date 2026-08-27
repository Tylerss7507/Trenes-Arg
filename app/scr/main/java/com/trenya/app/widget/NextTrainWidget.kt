package com.trenya.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.trenya.app.R
import com.trenya.app.TrenYaApplication
import com.trenya.app.data.model.DataResult
import kotlinx.coroutines.flow.first

/**
 * Widget deliberadamente simple (una columna, texto plano) para minimizar
 * superficie de fallas: muestra la estación favorita principal y el
 * countdown de su próximo tren. Se actualiza cada ~30 min (ver
 * next_train_widget_info.xml) y cuando el usuario lo toca.
 */
class NextTrainWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as TrenYaApplication).container
        val favorite = runCatching { container.userPreferencesRepository.favoritesFlow.first() }
            .getOrDefault(emptyList())
            .firstOrNull()

        val stationName = favorite?.stationName
        var countdownText = context.getString(R.string.widget_no_favorite)
        var destinationText: String? = null

        if (favorite != null) {
            val result = container.trainRepository.getUpcomingTrains(favorite.stationId, cantidad = 1)
            val train = (result as? DataResult.Success)?.data?.firstOrNull()
            countdownText = when {
                train == null -> context.getString(R.string.status_unknown)
                (train.secondsRemaining ?: Long.MAX_VALUE) <= 30 -> context.getString(R.string.station_arrived)
                else -> context.getString(R.string.minutes_short, ((train.secondsRemaining ?: 0) / 60).toInt())
            }
            destinationText = train?.let { context.getString(R.string.station_towards, it.destinationName) }
        }

        provideContent {
            WidgetContent(stationName = stationName, countdown = countdownText, destination = destinationText)
        }
    }
}

@Composable
private fun WidgetContent(stationName: String?, countdown: String, destination: String?) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = stationName ?: "TrenYa", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp))
        Text(text = countdown, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp))
        if (destination != null) {
            Text(
                text = destination,
                style = TextStyle(fontSize = 12.sp, color = ColorProvider(day = Color.DarkGray, night = Color.LightGray))
            )
        }
    }
}

class NextTrainWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextTrainWidget()
}
