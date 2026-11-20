package uk.ac.tees.mad.s3640901.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import uk.ac.tees.mad.s3640901.data.MedicationEntity

object MedicationReminderScheduler {

    const val ACTION_DOSE_REMINDER =
        "uk.ac.tees.mad.s3640901.action.DOSE_REMINDER"

    const val ACTION_MISSED_CHECK =
        "uk.ac.tees.mad.s3640901.action.MISSED_CHECK"

    const val EXTRA_MEDICATION_ID = "extra_medication_id"
    const val EXTRA_MEDICATION_NAME = "extra_medication_name"
    const val EXTRA_SCHEDULED_AT = "extra_scheduled_at"

    private const val ONE_HOUR_MILLIS = 60L * 60L * 1000L

    fun scheduleForMedication(
        context: Context,
        medication: MedicationEntity,
        afterMillis: Long = System.currentTimeMillis()
    ) {
        if (!medication.isActive) return

        val nextDose = MedicationScheduleCalculator.nextDoseTime(
            medication = medication,
            afterMillis = afterMillis
        ) ?: return

        scheduleDose(
            context = context,
            medication = medication,
            scheduledAt = nextDose,
            nowMillis = afterMillis
        )
    }

    fun scheduleNextForMedication(
        context: Context,
        medication: MedicationEntity,
        afterMillis: Long
    ) {
        scheduleForMedication(
            context = context,
            medication = medication,
            afterMillis = afterMillis
        )
    }

    fun rescheduleAll(
        context: Context,
        medications: List<MedicationEntity>
    ) {
        medications
            .filter { it.isActive }
            .forEach { medication ->
                scheduleForMedication(
                    context = context,
                    medication = medication
                )
            }
    }

    fun cancelForMedication(
        context: Context,
        medication: MedicationEntity,
        scheduledAt: Long
    ) {
        val alarmManager =
            context.getSystemService(AlarmManager::class.java)
                ?: return

        listOf(
            ACTION_DOSE_REMINDER,
            ACTION_MISSED_CHECK
        ).forEach { action ->

            alarmManager.cancel(
                createPendingIntent(
                    context = context,
                    medication = medication,
                    scheduledAt = scheduledAt,
                    action = action
                )
            )
        }
    }

    private fun scheduleDose(
        context: Context,
        medication: MedicationEntity,
        scheduledAt: Long,
        nowMillis: Long
    ) {
        val reminderTime = scheduledAt - ONE_HOUR_MILLIS

        if (reminderTime > nowMillis) {
            scheduleAlarm(
                context = context,
                triggerAtMillis = reminderTime,
                pendingIntent = createPendingIntent(
                    context = context,
                    medication = medication,
                    scheduledAt = scheduledAt,
                    action = ACTION_DOSE_REMINDER
                )
            )
        }

        val missedCheckTime = scheduledAt + ONE_HOUR_MILLIS

        if (missedCheckTime > nowMillis) {
            scheduleAlarm(
                context = context,
                triggerAtMillis = missedCheckTime,
                pendingIntent = createPendingIntent(
                    context = context,
                    medication = medication,
                    scheduledAt = scheduledAt,
                    action = ACTION_MISSED_CHECK
                )
            )
        }
    }

    private fun scheduleAlarm(
        context: Context,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        val alarmManager =
            context.getSystemService(AlarmManager::class.java)
                ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun createPendingIntent(
        context: Context,
        medication: MedicationEntity,
        scheduledAt: Long,
        action: String
    ): PendingIntent {
        val intent = Intent(
            context,
            MedicationReminderReceiver::class.java
        ).apply {
            this.action = action
            putExtra(EXTRA_MEDICATION_ID, medication.id)
            putExtra(EXTRA_MEDICATION_NAME, medication.name)
            putExtra(EXTRA_SCHEDULED_AT, scheduledAt)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode(
                medicationId = medication.id,
                scheduledAt = scheduledAt,
                action = action
            ),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(
        medicationId: Long,
        scheduledAt: Long,
        action: String
    ): Int {
        return "${medicationId}_${scheduledAt}_$action"
            .hashCode() and 0xFFFF
    }
}