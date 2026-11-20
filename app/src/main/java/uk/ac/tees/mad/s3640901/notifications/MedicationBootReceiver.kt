package uk.ac.tees.mad.s3640901.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uk.ac.tees.mad.s3640901.data.MedMinderDatabase

/**
 * Restores medication alarms after the device restarts.
 */
class MedicationBootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        ).launch {
            try {
                val dao = MedMinderDatabase
                    .getInstance(appContext)
                    .medicationDao()

                MedicationReminderScheduler.rescheduleAll(
                    context = appContext,
                    medications = dao.getActiveMedications()
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}