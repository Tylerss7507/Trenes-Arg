package com.trenya.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.trenya.app.MainActivity
import com.trenya.app.R
import com.trenya.app.core.Constants

class NotificationHelper(private val context: Context) {

    init {
        createChannels()
    }

    private fun createChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val delays = NotificationChannel(
            Constants.NOTIF_CHANNEL_DELAYS,
            context.getString(R.string.notif_channel_delays_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.notif_channel_delays_desc) }

        val general = NotificationChannel(
            Constants.NOTIF_CHANNEL_GENERAL,
            context.getString(R.string.notif_channel_general_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.notif_channel_general_desc) }

        manager.createNotificationChannels(listOf(delays, general))
    }

    /** [notificationId] debería ser estable por estación/servicio para poder actualizar en vez de duplicar avisos. */
    fun notifyDelay(stationName: String, message: String, notificationId: Int) {
        val hasPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIF_CHANNEL_DELAYS)
            .setSmallIcon(R.drawable.ic_train)
            .setColor(ContextCompat.getColor(context, R.color.notification_color))
            .setContentTitle(context.getString(R.string.notif_delay_title, stationName, message))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
