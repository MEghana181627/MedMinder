package uk.ac.tees.mad.s3640901.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uk.ac.tees.mad.s3640901.data.DoseStatus
import uk.ac.tees.mad.s3640901.data.MedMinderDatabase
import uk.ac.tees.mad.s3640901.data.MedicationDoseOutcomeEntity

class MedicationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        ).launch {
            try {
                val medicationId = intent.getLongExtra(
                    MedicationReminderScheduler.EXTRA_MEDICATION_ID,
                    0L
                )

                val scheduledAt = intent.getLongExtra(
                    MedicationReminderScheduler.EXTRA_SCHEDULED_AT,
                    0L
                )

                if (
                    medicationId == 0L ||
                    scheduledAt == 0L
                ) {
                    return@launch
                }

                val dao = MedMinderDatabase
                    .getInstance(appContext)
                    .medicationDao()

                val medication = dao
                    .getActiveMedications()
                    .firstOrNull { it.id == medicationId }
                    ?: return@launch

                // Ignore stale alarms for doses before medication creation.
                if (scheduledAt < medication.createdAt) {
                    MedicationReminderScheduler.scheduleForMedication(
                        context = appContext,
                        medication = medication,
                        afterMillis = System.currentTimeMillis()
                    )
                    return@launch
                }

                val existingOutcome =
                    dao.findDoseOutcome(
                        medicationId = medicationId,
                        scheduledAt = scheduledAt
                    )

                when (intent.action) {

                    MedicationReminderScheduler.ACTION_DOSE_REMINDER -> {
                        if (existingOutcome == null) {
                            MedicationNotification.showDoseReminder(
                                context = appContext,
                                medicationName = medication.name,
                                scheduledAt = scheduledAt,
                                notificationId = notificationId(
                                    medicationId = medicationId,
                                    scheduledAt = scheduledAt,
                                    type = "reminder"
                                )
                            )
                        }
                    }

                    MedicationReminderScheduler.ACTION_MISSED_CHECK -> {
                        if (existingOutcome == null) {
                            dao.saveDoseOutcome(
                                MedicationDoseOutcomeEntity(
                                    medicationId = medicationId,
                                    scheduledAt = scheduledAt,
                                    status = DoseStatus.MISSED,
                                    recordedAt = System.currentTimeMillis()
                                )
                            )

                            MedicationNotification.showMissedDose(
                                context = appContext,
                                medicationName = medication.name,
                                scheduledAt = scheduledAt,
                                notificationId = notificationId(
                                    medicationId = medicationId,
                                    scheduledAt = scheduledAt,
                                    type = "missed"
                                )
                            )
                        }

                        MedicationReminderScheduler
                            .scheduleNextForMedication(
                                context = appContext,
                                medication = medication,
                                afterMillis = scheduledAt + 1L
                            )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun notificationId(
        medicationId: Long,
        scheduledAt: Long,
        type: String
    ): Int {
        return "${medicationId}_${scheduledAt}_$type".hashCode()
    }
}