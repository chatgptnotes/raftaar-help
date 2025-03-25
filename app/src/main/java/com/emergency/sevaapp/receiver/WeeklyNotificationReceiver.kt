package com.emergency.sevaapp.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.emergency.sevaapp.R
import com.emergency.sevaapp.activities.MainActivity

class WeeklyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Create the notification
        val notificationIntent = Intent(context, MainActivity::class.java)

        // Use FLAG_IMMUTABLE as required for Android 12+
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "weekly_notifications")
            .setSmallIcon(R.drawable.raftaar_seva_logo) // Replace with your app logo
            .setContentTitle("🚑 Reminder: Health Check!")
            .setContentText("It's time for a quick health check! Tap to open the app.")
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(0, builder.build())  // Use a unique notification ID
    }
}