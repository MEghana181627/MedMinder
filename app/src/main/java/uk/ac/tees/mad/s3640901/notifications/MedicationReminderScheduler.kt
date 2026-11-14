package uk.ac.tees.mad.s3640901.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import uk.ac.tees.mad.s3640901.data.MedicationEntity

object MedicationReminderScheduler {

    private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L

    fun scheduleNext(
        context: Context,
        medication: MedicationEntity,
        afterMillis: Long = System.currentTimeMillis()
    ) {
        if (medication.id <= 0L) {
            return
        }

        val doseTime =
            MedicationScheduleCalculator.nextDoseTime(
                medication = medication,
                afterMillis = afterMillis
            ) ?: return

        val currentTime = System.currentTimeMillis()
        val reminderTime = doseTime - ONE_HOUR_MILLIS

        val triggerTime = when {
            reminderTime > currentTime -> reminderTime
            doseTime > currentTime -> currentTime + 1_000L
            else -> return
        }

        val reminderIntent = Intent(
            context,
            MedicationReminderReceiver::class.java
        ).apply {
            putExtra(
                MedicationReminderReceiver.EXTRA_MEDICATION_ID,
                medication.id
            )
            putExtra(
                MedicationReminderReceiver.EXTRA_MEDICINE_NAME,
                medication.name
            )
            putExtra(
                MedicationReminderReceiver.EXTRA_DOSAGE,
                medication.dosage
            )
            putExtra(
                MedicationReminderReceiver.EXTRA_SCHEDULE_TYPE,
                medication.scheduleType
            )
            putExtra(
                MedicationReminderReceiver.EXTRA_SCHEDULE_VALUE,
                medication.scheduleValue
            )
            putExtra(
                MedicationReminderReceiver.EXTRA_DOSE_TIME,
                doseTime
            )
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medication.id.toInt(),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        alarmManager.cancel(pendingIntent)

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun cancel(
        context: Context,
        medicationId: Long
    ) {
        val reminderIntent = Intent(
            context,
            MedicationReminderReceiver::class.java
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.toInt(),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}