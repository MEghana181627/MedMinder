package uk.ac.tees.mad.s3640901.notifications

import android.content.Context
import uk.ac.tees.mad.s3640901.data.DoseStatus
import uk.ac.tees.mad.s3640901.data.MedicationDao
import uk.ac.tees.mad.s3640901.data.MedicationDoseOutcomeEntity
import uk.ac.tees.mad.s3640901.data.MedicationEntity

object MedicationOverdueProcessor {

    private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L

    suspend fun process(
        context: Context,
        medicationDao: MedicationDao,
        medications: List<MedicationEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        medications
            .filter { it.isActive }
            .forEach { medication ->

                val previousDose =
                    MedicationScheduleCalculator.previousDoseTime(
                        medication = medication,
                        beforeMillis = nowMillis,
                        notBeforeMillis = medication.createdAt
                    )

                val isOverdue =
                    previousDose != null &&
                            nowMillis >= previousDose + ONE_HOUR_MILLIS

                val hasOutcome =
                    previousDose?.let {
                        medicationDao.findDoseOutcome(
                            medicationId = medication.id,
                            scheduledAt = it
                        )
                    } != null

                if (
                    previousDose != null &&
                    isOverdue &&
                    !hasOutcome
                ) {
                    medicationDao.saveDoseOutcome(
                        MedicationDoseOutcomeEntity(
                            medicationId = medication.id,
                            scheduledAt = previousDose,
                            status = DoseStatus.MISSED,
                            recordedAt = nowMillis
                        )
                    )

                    MedicationNotification.showMissedDose(
                        context = context,
                        medicationName = medication.name,
                        scheduledAt = previousDose,
                        notificationId =
                            "${medication.id}_${previousDose}_missed"
                                .hashCode()
                    )
                }

                MedicationReminderScheduler.scheduleForMedication(
                    context = context,
                    medication = medication,
                    afterMillis = nowMillis
                )
            }
    }
}