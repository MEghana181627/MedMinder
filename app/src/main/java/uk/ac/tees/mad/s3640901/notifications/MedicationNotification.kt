package uk.ac.tees.mad.s3640901.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import uk.ac.tees.mad.s3640901.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MedicationNotification {

    const val CHANNEL_ID = "medication_reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Medication reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Dose and missed-medication reminders"
        }

        val notificationManager =
            context.getSystemService(NotificationManager::class.java)

        notificationManager?.createNotificationChannel(channel)
    }

    fun showDoseReminder(
        context: Context,
        medicationName: String,
        scheduledAt: Long,
        notificationId: Int
    ) {
        showNotification(
            context = context,
            title = "Medication reminder",
            message = "A dose of $medicationName is scheduled at ${
                formatTime(scheduledAt)
            }.",
            notificationId = notificationId
        )
    }

    fun showMissedDose(
        context: Context,
        medicationName: String,
        scheduledAt: Long,
        notificationId: Int
    ) {
        showNotification(
            context = context,
            title = "Medication missed",
            message = "The scheduled dose of $medicationName at ${
                formatTime(scheduledAt)
            } was missed.",
            notificationId = notificationId
        )
    }

    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int
    ) {
        createChannel(context)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat
                .from(context)
                .notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission may have been revoked while the app was running.
        }
    }

    private fun formatTime(timeMillis: Long): String {
        return SimpleDateFormat(
            "EEE, dd MMM HH:mm",
            Locale.getDefault()
        ).format(Date(timeMillis))
    }
}