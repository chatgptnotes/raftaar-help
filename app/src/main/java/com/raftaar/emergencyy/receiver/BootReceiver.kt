package com.raftaar.emergencyy.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.raftaar.emergencyy.activities.MainActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val alarmIntent = Intent(context, WeeklyNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                alarmIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + MainActivity.NOTIFICATION_INTERVAL,
                MainActivity.NOTIFICATION_INTERVAL,
                pendingIntent
            )
        }
    }
}
