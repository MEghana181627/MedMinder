package uk.ac.tees.mad.s3640901.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uk.ac.tees.mad.s3640901.data.MedicationEntity

class MedicationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val medicationId = intent.getLongExtra(
            EXTRA_MEDICATION_ID,
            0L
        )

        val medicineName = intent.getStringExtra(
            EXTRA_MEDICINE_NAME
        ).orEmpty()

        val dosage = intent.getStringExtra(
            EXTRA_DOSAGE
        ).orEmpty()

        val scheduleType = intent.getStringExtra(
            EXTRA_SCHEDULE_TYPE
        ).orEmpty()

        val scheduleValue = intent.getStringExtra(
            EXTRA_SCHEDULE_VALUE
        ).orEmpty()

        val doseTime = intent.getLongExtra(
            EXTRA_DOSE_TIME,
            System.currentTimeMillis()
        )

        if (medicationId <= 0L || medicineName.isBlank()) {
            return
        }

        MedicationNotification.showReminder(
            context = context,
            medicationId = medicationId,
            medicineName = medicineName,
            dosage = dosage
        )

        val medication = MedicationEntity(
            id = medicationId,
            name = medicineName,
            dosage = dosage,
            instructions = "",
            scheduleType = scheduleType,
            scheduleValue = scheduleValue,
            isActive = true,
            photoPath = null
        )

        MedicationReminderScheduler.scheduleNext(
            context = context,
            medication = medication,
            afterMillis = doseTime
        )
    }

    companion object {
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_SCHEDULE_TYPE = "schedule_type"
        const val EXTRA_SCHEDULE_VALUE = "schedule_value"
        const val EXTRA_DOSE_TIME = "dose_time"
    }
}