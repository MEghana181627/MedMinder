package uk.ac.tees.mad.s3640901

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.ac.tees.mad.s3640901.api.MedicineRepository
import uk.ac.tees.mad.s3640901.data.MedMinderDatabase
import uk.ac.tees.mad.s3640901.notifications.MedicationNotification
import uk.ac.tees.mad.s3640901.notifications.MedicationOverdueProcessor
import uk.ac.tees.mad.s3640901.ui.navigation.MedMinderNavGraph
import uk.ac.tees.mad.s3640901.ui.theme.MedMinderTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MedMinderTheme {
                MedMinderApp()
            }
        }
    }
}

@Composable
private fun MedMinderApp() {
    val context = LocalContext.current

    val database = remember(context) {
        MedMinderDatabase.getInstance(context)
    }

    val medicationDao = remember(database) {
        database.medicationDao()
    }

    val medicineRepository = remember {
        MedicineRepository()
    }

    LaunchedEffect(context, medicationDao) {
        MedicationNotification.createChannel(context)

        withContext(Dispatchers.IO) {
            val medications = medicationDao.getActiveMedications()

            MedicationOverdueProcessor.process(
                context = context,
                medicationDao = medicationDao,
                medications = medications
            )
        }
    }

    MedMinderNavGraph(
        medicationDao = medicationDao,
        medicineRepository = medicineRepository
    )
}