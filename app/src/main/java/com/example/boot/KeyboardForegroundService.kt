package com.example.boot

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.settings.SettingsActivity
import java.util.concurrent.TimeUnit

class KeyboardForegroundService : Service() {
    
    companion object {
        const val CHANNEL_ID = "CompBoardServiceChannel"
        const val WARNING_CHANNEL_ID = "CompBoardWarningChannel"
        const val NOTIFICATION_ID = 1001
        const val WARNING_NOTIFICATION_ID = 1002
    }

    private val handler = Handler(Looper.getMainLooper())
    private val permissionCheckRunnable = object : Runnable {
        override fun run() {
            checkPermissions()
            handler.postDelayed(this, 15 * 60 * 1000) // Check every 15 mins
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleWatchdogs()
        handler.post(permissionCheckRunnable)
    }

    private fun scheduleWatchdogs() {
        // WorkManager for 15-min periodic checks
        val workRequest = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ServiceWatchdog",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        // AlarmManager for 5-min checks
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val broadcastIntent = Intent(this, BootReceiver::class.java).apply {
            action = "com.example.boot.RESTART_SERVICE"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, broadcastIntent, PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 5 * 60 * 1000,
            5 * 60 * 1000,
            pendingIntent
        )
    }

    private fun checkPermissions() {
        val requiredPermissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        // Check standard permissions
        val hasMissing = requiredPermissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        // Check System Alert Window (Overlay)
        val canDrawOverlays = Settings.canDrawOverlays(this)
        
        if (hasMissing || !canDrawOverlays) {
            showPermissionWarning()
        }
    }

    private fun showPermissionWarning() {
        val notificationIntent = Intent(this, SettingsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, WARNING_CHANNEL_ID)
            .setContentTitle("Permissions Revoked")
            .setContentText("CompBoard is missing required permissions. Tap to fix.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(WARNING_NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, SettingsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CompBoard Keyboard Active")
            .setContentText("Tap to open settings")
            .setSmallIcon(android.R.drawable.ic_menu_preferences) // Placeholder
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        val broadcastIntent = Intent(this, BootReceiver::class.java).apply {
            action = "com.example.boot.RESTART_SERVICE"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            broadcastIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.set(
            android.app.AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 5000, // Restart in 5 seconds
            pendingIntent
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "CompBoard Running",
                NotificationManager.IMPORTANCE_LOW
            )
            manager?.createNotificationChannel(serviceChannel)
            
            val warningChannel = NotificationChannel(
                WARNING_CHANNEL_ID,
                "CompBoard Warnings",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager?.createNotificationChannel(warningChannel)
        }
    }
}
