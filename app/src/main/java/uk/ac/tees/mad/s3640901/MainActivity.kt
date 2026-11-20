@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package uk.ac.tees.mad.s3640901

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.tees.mad.s3640901.api.MedicineRepository
import uk.ac.tees.mad.s3640901.api.MedicineSuggestion
import uk.ac.tees.mad.s3640901.data.DoseStatus
import uk.ac.tees.mad.s3640901.data.MedMinderDatabase
import uk.ac.tees.mad.s3640901.data.MedicationDao
import uk.ac.tees.mad.s3640901.data.MedicationDoseOutcomeEntity
import uk.ac.tees.mad.s3640901.data.MedicationEntity
import uk.ac.tees.mad.s3640901.notifications.MedicationNotification
import uk.ac.tees.mad.s3640901.notifications.MedicationOverdueProcessor
import uk.ac.tees.mad.s3640901.notifications.MedicationReminderScheduler
import uk.ac.tees.mad.s3640901.notifications.MedicationScheduleCalculator
import uk.ac.tees.mad.s3640901.ui.theme.MedMinderTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

private object Routes {
    const val SECURE_ACCESS = "secure_access"
    const val DASHBOARD = "dashboard"
    const val ADD_MEDICATION = "add_medication"
    const val HISTORY = "history"
}

private enum class ScheduleType(
    val label: String
) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    ONE_TIME("One-time")
}

@Composable
private fun MedMinderApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    val medicationDao = remember {
        MedMinderDatabase
            .getInstance(context)
            .medicationDao()
    }

    LaunchedEffect(Unit) {
        MedicationNotification.createChannel(context)
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SECURE_ACCESS
    ) {
        composable(Routes.SECURE_ACCESS) {
            SecureAccessScreen(
                onAuthenticated = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SECURE_ACCESS) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                navController = navController,
                medicationDao = medicationDao
            )
        }

        composable(Routes.ADD_MEDICATION) {
            AddMedicationScreen(
                medicationDao = medicationDao,
                onBack = {
                    navController.popBackStack()
                },
                onSaved = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                medicationDao = medicationDao,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun SecureAccessScreen(
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var message by rememberSaveable {
        mutableStateOf("")
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            onAuthenticated()
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MedMinder",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Secure Access",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = "Authenticate to view your medication information.",
            modifier = Modifier.padding(top = 12.dp)
        )

        Button(
            onClick = {
                if (activity == null) {
                    message = "Authentication is unavailable."
                    return@Button
                }

                showBiometricPrompt(
                    activity = activity,
                    onSuccess = {
                        if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else {
                            onAuthenticated()
                        }
                    },
                    onMessage = {
                        message = it
                    }
                )
            },
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Unlock with biometrics")
        }

        if (message.isNotBlank()) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onMessage: (String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)

    val promptBuilder = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock MedMinder")
        .setSubtitle("Authenticate to view your medication information")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        promptBuilder.setAllowedAuthenticators(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
    } else {
        promptBuilder
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
            .setNegativeButtonText("Cancel")
    }

    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                onSuccess()
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                onMessage(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onMessage("Authentication was not recognised. Try again.")
            }
        }
    )

    biometricPrompt.authenticate(promptBuilder.build())
}

@Composable
private fun DashboardScreen(
    navController: NavHostController,
    medicationDao: MedicationDao
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val medications by medicationDao
        .observeActiveMedications()
        .collectAsState(initial = emptyList())

    val outcomes by medicationDao
        .observeDoseOutcomes()
        .collectAsState(initial = emptyList())

    LaunchedEffect(medications) {
        withContext(Dispatchers.IO) {
            MedicationOverdueProcessor.process(
                context = context,
                medicationDao = medicationDao,
                medications = medications
            )
        }
    }

    val medicationById = medications.associateBy {
        it.id
    }

    val missedOutcomes = outcomes
        .filter {
            it.status == DoseStatus.MISSED &&
                    it.dismissedAt == null
        }
        .mapNotNull { outcome ->
            medicationById[outcome.medicationId]
                ?.let { medication ->
                    medication to outcome
                }
        }
        .sortedByDescending {
            it.second.scheduledAt
        }

    val upcomingDoses = medications
        .mapNotNull { medication ->
            val nextDose =
                MedicationScheduleCalculator.nextDoseTime(
                    medication = medication
                ) ?: return@mapNotNull null

            val outcome = outcomes.firstOrNull {
                it.medicationId == medication.id &&
                        it.scheduledAt == nextDose
            }

            Triple(
                medication,
                nextDose,
                outcome
            )
        }
        .filter {
            it.third == null
        }
        .sortedBy {
            it.second
        }

    fun saveStatus(
        medication: MedicationEntity,
        scheduledAt: Long,
        status: String
    ) {
        scope.launch {
            withContext(Dispatchers.IO) {
                medicationDao.saveDoseOutcome(
                    MedicationDoseOutcomeEntity(
                        medicationId = medication.id,
                        scheduledAt = scheduledAt,
                        status = status,
                        recordedAt = System.currentTimeMillis()
                    )
                )

                MedicationReminderScheduler
                    .scheduleNextForMedication(
                        context = context,
                        medication = medication,
                        afterMillis = scheduledAt + 1L
                    )
            }
        }
    }

    fun dismissMissedDose(
        outcome: MedicationDoseOutcomeEntity
    ) {
        scope.launch {
            withContext(Dispatchers.IO) {
                medicationDao.dismissDoseOutcome(
                    medicationId = outcome.medicationId,
                    scheduledAt = outcome.scheduledAt,
                    dismissedAt = System.currentTimeMillis()
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Medication Dashboard")
                }
            )
        }
    ) { paddingValues ->

        if (medications.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No active medication",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Add a medication to begin managing your schedule.",
                    modifier = Modifier.padding(top = 12.dp)
                )

                Button(
                    onClick = {
                        navController.navigate(
                            Routes.ADD_MEDICATION
                        )
                    },
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text("Add Medication")
                }

                OutlinedButton(
                    onClick = {
                        navController.navigate(Routes.HISTORY)
                    },
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text("My History")
                }
            }

            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (missedOutcomes.isNotEmpty()) {
                item {
                    Text(
                        text = "Missed Medications",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(
                    items = missedOutcomes,
                    key = { pair ->
                        "${pair.second.medicationId}_${pair.second.scheduledAt}"
                    }
                ) { pair ->
                    MissedMedicationCard(
                        medication = pair.first,
                        outcome = pair.second,
                        onDismiss = {
                            dismissMissedDose(pair.second)
                        }
                    )
                }
            }

            item {
                Text(
                    text = "Upcoming Medications",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (upcomingDoses.isEmpty()) {
                item {
                    Text("No upcoming medication doses.")
                }
            } else {
                items(
                    items = upcomingDoses,
                    key = { triple ->
                        "${triple.first.id}_${triple.second}"
                    }
                ) { triple ->
                    MedicationDoseCard(
                        medication = triple.first,
                        scheduledAt = triple.second,
                        currentStatus = triple.third?.status,
                        onStatusSelected = { status ->
                            saveStatus(
                                medication = triple.first,
                                scheduledAt = triple.second,
                                status = status
                            )
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        navController.navigate(
                            Routes.ADD_MEDICATION
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Another Medication")
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        navController.navigate(Routes.HISTORY)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("My History")
                }
            }
        }
    }
}

@Composable
private fun MedicationDoseCard(
    medication: MedicationEntity,
    scheduledAt: Long,
    currentStatus: String?,
    onStatusSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                MedicationThumbnail(
                    photoPath = medication.photoPath,
                    modifier = Modifier
                        .width(72.dp)
                        .height(72.dp)
                )

                Column {
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text("Dosage: ${medication.dosage}")

                    if (medication.instructions.isNotBlank()) {
                        Text(
                            "Instructions: ${medication.instructions}"
                        )
                    }

                    Text("Schedule: ${medication.scheduleType}")
                    Text(medication.scheduleValue)
                }
            }

            Text(
                text = "Next dose: ${formatDoseTime(scheduledAt)}",
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "Status: ${formatStatus(currentStatus)}",
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onStatusSelected(DoseStatus.TAKEN)
                    }
                ) {
                    Text("Taken")
                }

                Button(
                    onClick = {
                        onStatusSelected(DoseStatus.SKIPPED)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Skipped")
                }

                Button(
                    onClick = {
                        onStatusSelected(DoseStatus.MISSED)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Missed")
                }
            }
        }
    }
}

@Composable
private fun MissedMedicationCard(
    medication: MedicationEntity,
    outcome: MedicationDoseOutcomeEntity,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                MedicationThumbnail(
                    photoPath = medication.photoPath,
                    modifier = Modifier
                        .width(72.dp)
                        .height(72.dp)
                )

                Column {
                    Text(
                        text = medication.name,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Scheduled: ${
                            formatDoseTime(outcome.scheduledAt)
                        }",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Text(
                        text = "Status: Missed",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun MedicationThumbnail(
    photoPath: String?,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(photoPath) {
        photoPath
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                BitmapFactory.decodeFile(it)
            }
            ?.asImageBitmap()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Medicine photo",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(
                RoundedCornerShape(10.dp)
            )
        )
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No photo",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun AddMedicationScreen(
    medicationDao: MedicationDao,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val medicineRepository = remember {
        MedicineRepository()
    }

    var name by rememberSaveable {
        mutableStateOf("")
    }

    var dosage by rememberSaveable {
        mutableStateOf("")
    }

    var instructions by rememberSaveable {
        mutableStateOf("")
    }

    var scheduleType by remember {
        mutableStateOf(ScheduleType.DAILY)
    }

    var scheduleMenuExpanded by remember {
        mutableStateOf(false)
    }

    var selectedTime by rememberSaveable {
        mutableStateOf("")
    }

    var selectedDate by rememberSaveable {
        mutableStateOf("")
    }

    var monthlyDay by rememberSaveable {
        mutableStateOf("")
    }

    var selectedDays by remember {
        mutableStateOf(emptySet<String>())
    }

    var errorMessage by rememberSaveable {
        mutableStateOf("")
    }

    var isSaving by rememberSaveable {
        mutableStateOf(false)
    }

    var photoPath by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var pendingPhotoPath by remember {
        mutableStateOf<String?>(null)
    }

    var suggestions by remember {
        mutableStateOf<List<MedicineSuggestion>>(emptyList())
    }

    var isSearching by rememberSaveable {
        mutableStateOf(false)
    }

    var suggestionMessage by rememberSaveable {
        mutableStateOf("")
    }

    var suggestionMessageIsError by rememberSaveable {
        mutableStateOf(false)
    }

    var skipNextMedicineSearch by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(name) {
        if (skipNextMedicineSearch) {
            skipNextMedicineSearch = false
            suggestions = emptyList()
            suggestionMessage = ""
            suggestionMessageIsError = false
            isSearching = false
            return@LaunchedEffect
        }

        val prefix = name.trim()

        suggestions = emptyList()
        suggestionMessage = ""
        suggestionMessageIsError = false

        if (prefix.length < 2) {
            isSearching = false
            return@LaunchedEffect
        }

        delay(400)

        isSearching = true

        try {
            medicineRepository
                .suggestMedicine(prefix)
                .onSuccess { result ->
                    suggestions = result

                    if (result.isEmpty()) {
                        suggestionMessage =
                            "No matching medicine found. " +
                                    "You can enter the name manually."
                        suggestionMessageIsError = false
                    } else {
                        suggestionMessage = ""
                        suggestionMessageIsError = false
                    }
                }
                .onFailure {
                    suggestions = emptyList()
                    suggestionMessage =
                        "Unable to connect. Check your internet connection."
                    suggestionMessageIsError = true
                }
        } finally {
            isSearching = false
        }
    }

    val takePictureLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { captured ->
            if (captured) {
                photoPath = pendingPhotoPath
            } else {
                pendingPhotoPath?.let {
                    File(it).delete()
                }
            }

            pendingPhotoPath = null
        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                launchCamera(
                    context = context,
                    onPathCreated = { path, uri ->
                        pendingPhotoPath = path
                        takePictureLauncher.launch(uri)
                    },
                    onError = {
                        errorMessage = it
                    }
                )
            } else {
                errorMessage =
                    "Camera permission is required to capture a medicine photo."
            }
        }

    fun openCamera() {
        val permissionGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            launchCamera(
                context = context,
                onPathCreated = { path, uri ->
                    pendingPhotoPath = path
                    takePictureLauncher.launch(uri)
                },
                onError = {
                    errorMessage = it
                }
            )
        } else {
            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    fun showTimePicker() {
        val now = Calendar.getInstance()

        TimePickerDialog(
            context,
            { _, hour, minute ->
                selectedTime = "%02d:%02d".format(hour, minute)
                errorMessage = ""
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun showDatePicker() {
        val now = Calendar.getInstance()

        DatePickerDialog(
            context,
            { _, year, month, day ->
                selectedDate =
                    "%04d-%02d-%02d".format(
                        year,
                        month + 1,
                        day
                    )
                errorMessage = ""
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Add Medication")
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = ""
                },
                label = {
                    Text("Medication name *")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (isSearching) {
                Text(
                    text = "Searching medicine names...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (suggestionMessage.isNotBlank()) {
                Text(
                    text = suggestionMessage,
                    color = if (suggestionMessageIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            suggestions.forEach { suggestion ->
                MedicineSuggestionCard(
                    suggestion = suggestion,
                    onSelected = {
                        skipNextMedicineSearch = true
                        name = suggestion.name
                        suggestions = emptyList()
                        suggestionMessage = ""
                        suggestionMessageIsError = false
                        isSearching = false
                    }
                )
            }

            OutlinedTextField(
                value = dosage,
                onValueChange = {
                    dosage = it
                    errorMessage = ""
                },
                label = {
                    Text("Dosage *")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = instructions,
                onValueChange = {
                    instructions = it
                },
                label = {
                    Text("Instructions (optional)")
                },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Medicine photo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = {
                    openCamera()
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (photoPath == null) {
                        "Take medicine photo"
                    } else {
                        "Retake medicine photo"
                    }
                )
            }

            photoPath?.let { path ->
                val previewBitmap = remember(path) {
                    BitmapFactory
                        .decodeFile(path)
                        ?.asImageBitmap()
                }

                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = "Captured medicine photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(
                                RoundedCornerShape(12.dp)
                            )
                    )

                    OutlinedButton(
                        onClick = {
                            File(path).delete()
                            photoPath = null
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove photo")
                    }
                }
            }

            Text(
                text = "Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Box {
                OutlinedButton(
                    onClick = {
                        scheduleMenuExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Schedule type: ${scheduleType.label}"
                    )
                }

                DropdownMenu(
                    expanded = scheduleMenuExpanded,
                    onDismissRequest = {
                        scheduleMenuExpanded = false
                    }
                ) {
                    ScheduleType.values().forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(type.label)
                            },
                            onClick = {
                                scheduleType = type
                                scheduleMenuExpanded = false
                                errorMessage = ""
                            }
                        )
                    }
                }
            }

            if (scheduleType == ScheduleType.WEEKLY) {
                Text("Select weekdays")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Mon",
                        "Tue",
                        "Wed",
                        "Thu",
                        "Fri",
                        "Sat",
                        "Sun"
                    ).forEach { day ->

                        FilterChip(
                            selected = day in selectedDays,
                            onClick = {
                                selectedDays =
                                    if (day in selectedDays) {
                                        selectedDays - day
                                    } else {
                                        selectedDays + day
                                    }
                            },
                            label = {
                                Text(day)
                            }
                        )
                    }
                }
            }

            if (scheduleType == ScheduleType.MONTHLY) {
                OutlinedTextField(
                    value = monthlyDay,
                    onValueChange = {
                        monthlyDay = it.filter(Char::isDigit)
                        errorMessage = ""
                    },
                    label = {
                        Text("Day of month (1–31) *")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (scheduleType == ScheduleType.ONE_TIME) {
                OutlinedButton(
                    onClick = {
                        showDatePicker()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (selectedDate.isBlank()) {
                            "Select date *"
                        } else {
                            "Date: $selectedDate"
                        }
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    showTimePicker()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (selectedTime.isBlank()) {
                        "Select dose time *"
                    } else {
                        "Dose time: $selectedTime"
                    }
                )
            }

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    enabled = !isSaving,
                    onClick = {
                        val validationError =
                            validateMedication(
                                name = name,
                                dosage = dosage,
                                scheduleType = scheduleType,
                                selectedDays = selectedDays,
                                monthlyDay = monthlyDay,
                                selectedDate = selectedDate,
                                selectedTime = selectedTime
                            )

                        if (validationError != null) {
                            errorMessage = validationError
                            return@Button
                        }

                        val scheduleValue =
                            buildScheduleValue(
                                scheduleType = scheduleType,
                                selectedDays = selectedDays,
                                monthlyDay = monthlyDay,
                                selectedDate = selectedDate,
                                selectedTime = selectedTime
                            )

                        isSaving = true
                        errorMessage = ""

                        scope.launch {
                            try {
                                val savedMedication =
                                    withContext(Dispatchers.IO) {
                                        val medication =
                                            MedicationEntity(
                                                name = name.trim(),
                                                dosage = dosage.trim(),
                                                instructions = instructions.trim(),
                                                scheduleType = scheduleType.label,
                                                scheduleValue = scheduleValue,
                                                isActive = true,
                                                photoPath = photoPath,
                                                createdAt = System.currentTimeMillis()
                                            )

                                        val id =
                                            medicationDao.insertMedication(
                                                medication
                                            )

                                        medication.copy(id = id)
                                    }

                                MedicationReminderScheduler
                                    .scheduleForMedication(
                                        context = context,
                                        medication = savedMedication
                                    )

                                onSaved()
                            } catch (exception: Exception) {
                                errorMessage =
                                    "Medication could not be saved."
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                ) {
                    Text(
                        if (isSaving) {
                            "Saving..."
                        } else {
                            "Save Medication"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MedicineSuggestionCard(
    suggestion: MedicineSuggestion,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = suggestion.name,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = onSelected,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            ) {
                Text("Use")
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    medicationDao: MedicationDao,
    onBack: () -> Unit
) {
    val medications by medicationDao
        .observeActiveMedications()
        .collectAsState(initial = emptyList())

    val outcomes by medicationDao
        .observeDoseOutcomes()
        .collectAsState(initial = emptyList())

    val medicationById = medications.associateBy {
        it.id
    }

    val history = outcomes
        .mapNotNull { outcome ->
            medicationById[outcome.medicationId]
                ?.let { medication ->
                    medication to outcome
                }
        }
        .sortedByDescending {
            it.second.scheduledAt
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("My History")
                }
            )
        }
    ) { paddingValues ->

        if (history.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No medication history yet.")

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    Text("Back to Dashboard")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(history) { pair ->
                    HistoryCard(
                        medication = pair.first,
                        outcome = pair.second
                    )
                }

                item {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to Dashboard")
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    medication: MedicationEntity,
    outcome: MedicationDoseOutcomeEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = medication.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text("Dosage: ${medication.dosage}")
            Text(
                "Scheduled: ${formatDoseTime(outcome.scheduledAt)}"
            )
            Text(
                "Status: ${formatStatus(outcome.status)}"
            )
            Text(
                "Recorded: ${formatDoseTime(outcome.recordedAt)}"
            )
        }
    }
}

private fun launchCamera(
    context: Context,
    onPathCreated: (String, Uri) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val directory = File(
            context.filesDir,
            "medicine_photos"
        ).apply {
            if (!exists()) {
                mkdirs()
            }
        }

        val photoFile = File.createTempFile(
            "medicine_",
            ".jpg",
            directory
        )

        val photoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )

        onPathCreated(
            photoFile.absolutePath,
            photoUri
        )
    } catch (exception: Exception) {
        onError("Unable to open the camera.")
    }
}

private fun validateMedication(
    name: String,
    dosage: String,
    scheduleType: ScheduleType,
    selectedDays: Set<String>,
    monthlyDay: String,
    selectedDate: String,
    selectedTime: String
): String? {
    if (name.isBlank()) {
        return "Enter a medication name."
    }

    if (dosage.isBlank()) {
        return "Enter the dosage."
    }

    if (selectedTime.isBlank()) {
        return "Select a dose time."
    }

    if (
        scheduleType == ScheduleType.WEEKLY &&
        selectedDays.isEmpty()
    ) {
        return "Select at least one weekday."
    }

    if (scheduleType == ScheduleType.MONTHLY) {
        val day = monthlyDay.toIntOrNull()

        if (day == null || day !in 1..31) {
            return "Enter a day between 1 and 31."
        }
    }

    if (
        scheduleType == ScheduleType.ONE_TIME &&
        selectedDate.isBlank()
    ) {
        return "Select the medication date."
    }

    return null
}

private fun buildScheduleValue(
    scheduleType: ScheduleType,
    selectedDays: Set<String>,
    monthlyDay: String,
    selectedDate: String,
    selectedTime: String
): String {
    return when (scheduleType) {
        ScheduleType.DAILY ->
            "Every day at $selectedTime"

        ScheduleType.WEEKLY ->
            "${selectedDays.sorted().joinToString(", ")} at $selectedTime"

        ScheduleType.MONTHLY ->
            "Day $monthlyDay of each month at $selectedTime"

        ScheduleType.ONE_TIME ->
            "$selectedDate at $selectedTime"
    }
}

private fun formatStatus(status: String?): String {
    return status
        ?.lowercase(Locale.getDefault())
        ?.replaceFirstChar {
            it.uppercase()
        }
        ?: "Pending"
}

private fun formatDoseTime(timeMillis: Long): String {
    return SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm",
        Locale.getDefault()
    ).format(Date(timeMillis))
}