package uk.ac.tees.mad.s3640901

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import uk.ac.tees.mad.s3640901.data.MedicationEntity
import uk.ac.tees.mad.s3640901.data.MedMinderDatabase
import uk.ac.tees.mad.s3640901.notifications.MedicationNotification
import uk.ac.tees.mad.s3640901.notifications.MedicationReminderScheduler
import uk.ac.tees.mad.s3640901.ui.theme.MedMinderTheme
import java.io.File
import java.util.Calendar
import java.util.Locale

class MainActivity : FragmentActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 2201
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MedicationNotification.createChannel(this)

        setContent {
            MedMinderTheme {
                MedMinderApp()
            }
        }

        window.decorView.post {
            requestNotificationPermissionIfNeeded()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionState = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
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

private fun createMedicinePhotoFile(context: Context): File {
    val directory = File(
        context.filesDir,
        "medicine_photos"
    )

    if (!directory.exists()) {
        directory.mkdirs()
    }

    return File.createTempFile(
        "medicine_",
        ".jpg",
        directory
    )
}

@Composable
private fun MedMinderApp() {
    val context = LocalContext.current

    val database = remember {
        MedMinderDatabase.getInstance(context)
    }

    val navController = rememberNavController()

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
                database = database,
                navController = navController
            )
        }

        composable(Routes.ADD_MEDICATION) {
            AddMedicationScreen(
                database = database,
                onSaved = {
                    navController.popBackStack()
                },
                onCancelClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.HISTORY) {
            PlaceholderScreen(
                title = "My History",
                message = "Medication history will be implemented in a later sprint.",
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
                if (activity != null) {
                    showBiometricPrompt(
                        activity = activity,
                        onSuccess = onAuthenticated,
                        onMessage = {
                            message = it
                        }
                    )
                } else {
                    message = "Authentication is not available."
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    database: MedMinderDatabase,
    navController: NavHostController
) {
    val medications by database.medicationDao()
        .observeActiveMedications()
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Medication Dashboard")
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    navController.navigate(Routes.ADD_MEDICATION)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Medication")
            }

            if (medications.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
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
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(medications) { medication ->
                        MedicationCard(medication)
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    navController.navigate(Routes.HISTORY)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text("My History")
            }
        }
    }
}

@Composable
private fun MedicationCard(
    medication: MedicationEntity
) {
    val bitmap = remember(medication.photoPath) {
        medication.photoPath
            ?.takeIf { it.isNotBlank() }
            ?.let { BitmapFactory.decodeFile(it) }
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Dosage: ${medication.dosage}",
                    modifier = Modifier.padding(top = 6.dp)
                )

                Text(
                    text = "Schedule: ${medication.scheduleType}",
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = medication.scheduleValue,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (medication.instructions.isNotBlank()) {
                    Text(
                        text = "Instructions: ${medication.instructions}",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Medicine photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(76.dp)
                        .height(76.dp)
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMedicationScreen(
    database: MedMinderDatabase,
    onSaved: () -> Unit,
    onCancelClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var medicationName by rememberSaveable {
        mutableStateOf("")
    }

    var dosage by rememberSaveable {
        mutableStateOf("")
    }

    var instructions by rememberSaveable {
        mutableStateOf("")
    }

    var scheduleType by rememberSaveable {
        mutableStateOf("Daily")
    }

    var scheduleMenuExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var doseTime by rememberSaveable {
        mutableStateOf("")
    }

    var selectedWeekdays by rememberSaveable {
        mutableStateOf("")
    }

    var monthlyDay by rememberSaveable {
        mutableStateOf("")
    }

    var oneTimeDate by rememberSaveable {
        mutableStateOf("")
    }

    var photoPath by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var pendingPhotoPath by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var errorMessage by rememberSaveable {
        mutableStateOf("")
    }

    var cameraMessage by rememberSaveable {
        mutableStateOf("")
    }

    val weekdays = listOf(
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday"
    )

    val takePictureLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->

            val capturedPath = pendingPhotoPath

            if (success && capturedPath != null) {
                val oldPhotoPath = photoPath

                photoPath = capturedPath
                cameraMessage = ""

                if (
                    oldPhotoPath != null &&
                    oldPhotoPath != capturedPath
                ) {
                    File(oldPhotoPath).delete()
                }
            } else {
                capturedPath?.let {
                    File(it).delete()
                }

                cameraMessage = "Photo capture was cancelled."
            }

            pendingPhotoPath = null
        }

    val launchCamera: () -> Unit = {
        try {
            val imageFile = createMedicinePhotoFile(context)

            pendingPhotoPath = imageFile.absolutePath

            val imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            takePictureLauncher.launch(imageUri)
        } catch (_: Exception) {
            pendingPhotoPath = null
            cameraMessage = "Unable to open the camera."
        }
    }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                launchCamera()
            } else {
                cameraMessage =
                    "Camera permission is required to capture a medicine photo."
            }
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = medicationName,
                onValueChange = {
                    medicationName = it
                    errorMessage = ""
                },
                label = {
                    Text("Medication name")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dosage,
                onValueChange = {
                    dosage = it
                    errorMessage = ""
                },
                label = {
                    Text("Dosage")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )

            Button(
                onClick = {
                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        launchCamera()
                    } else {
                        cameraPermissionLauncher.launch(
                            Manifest.permission.CAMERA
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Take Medicine Photo")
            }

            if (photoPath != null) {
                Text(
                    text = "Photo preview",
                    fontWeight = FontWeight.Bold
                )

                val bitmap = remember(photoPath) {
                    photoPath
                        ?.takeIf { it.isNotBlank() }
                        ?.let { BitmapFactory.decodeFile(it) }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured medicine photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            }

            if (cameraMessage.isNotBlank()) {
                Text(
                    text = cameraMessage,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Schedule",
                fontWeight = FontWeight.Bold
            )

            Box {
                OutlinedButton(
                    onClick = {
                        scheduleMenuExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Schedule type: $scheduleType")
                }

                DropdownMenu(
                    expanded = scheduleMenuExpanded,
                    onDismissRequest = {
                        scheduleMenuExpanded = false
                    }
                ) {
                    listOf(
                        "Daily",
                        "Weekly",
                        "Monthly",
                        "One-time"
                    ).forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(type)
                            },
                            onClick = {
                                scheduleType = type
                                scheduleMenuExpanded = false
                            }
                        )
                    }
                }
            }

            if (scheduleType == "Weekly") {
                Text("Select weekdays")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    weekdays.forEach { day ->
                        val isSelected = selectedWeekdays
                            .split(",")
                            .contains(day)

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val currentDays = selectedWeekdays
                                    .split(",")
                                    .filter { it.isNotBlank() }
                                    .toMutableList()

                                if (currentDays.contains(day)) {
                                    currentDays.remove(day)
                                } else {
                                    currentDays.add(day)
                                }

                                selectedWeekdays =
                                    currentDays.joinToString(",")
                            },
                            label = {
                                Text(day.take(3))
                            },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }

            if (scheduleType == "Monthly") {
                OutlinedTextField(
                    value = monthlyDay,
                    onValueChange = {
                        monthlyDay = it.filter { character ->
                            character.isDigit()
                        }
                    },
                    label = {
                        Text("Day of month, 1-31")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (scheduleType == "One-time") {
                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance()

                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                oneTimeDate = String.format(
                                    Locale.getDefault(),
                                    "%02d/%02d/%04d",
                                    day,
                                    month + 1,
                                    year
                                )
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (oneTimeDate.isBlank()) {
                            "Select date *"
                        } else {
                            "Date: $oneTimeDate"
                        }
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    val calendar = Calendar.getInstance()

                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            doseTime = String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                hour,
                                minute
                            )

                            errorMessage = ""
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (doseTime.isBlank()) {
                        "Select dose time *"
                    } else {
                        "Dose time: $doseTime"
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
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = {
                        photoPath?.let {
                            File(it).delete()
                        }

                        pendingPhotoPath?.let {
                            File(it).delete()
                        }

                        onCancelClick()
                    }
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        when {
                            medicationName.isBlank() -> {
                                errorMessage =
                                    "Enter a medication name."
                            }

                            dosage.isBlank() -> {
                                errorMessage =
                                    "Enter the dosage."
                            }

                            scheduleType == "Weekly" &&
                                    selectedWeekdays.isBlank() -> {
                                errorMessage =
                                    "Select at least one weekday."
                            }

                            scheduleType == "Monthly" &&
                                    monthlyDay.toIntOrNull() !in 1..31 -> {
                                errorMessage =
                                    "Enter a day between 1 and 31."
                            }

                            scheduleType == "One-time" &&
                                    oneTimeDate.isBlank() -> {
                                errorMessage =
                                    "Select a date."
                            }

                            doseTime.isBlank() -> {
                                errorMessage =
                                    "Select a dose time."
                            }

                            else -> {
                                val scheduleValue =
                                    when (scheduleType) {
                                        "Daily" ->
                                            "Every day at $doseTime"

                                        "Weekly" ->
                                            "$selectedWeekdays at $doseTime"

                                        "Monthly" ->
                                            "Day $monthlyDay of each month at $doseTime"

                                        "One-time" ->
                                            "$oneTimeDate at $doseTime"

                                        else -> doseTime
                                    }

                                scope.launch {
                                    try {
                                        val medication =
                                            MedicationEntity(
                                                name = medicationName.trim(),
                                                dosage = dosage.trim(),
                                                instructions = instructions.trim(),
                                                scheduleType = scheduleType,
                                                scheduleValue = scheduleValue,
                                                isActive = true,
                                                photoPath = photoPath
                                            )

                                        val generatedId =
                                            database
                                                .medicationDao()
                                                .insertMedication(
                                                    medication
                                                )

                                        MedicationReminderScheduler
                                            .scheduleNext(
                                                context = context,
                                                medication = medication.copy(
                                                    id = generatedId
                                                )
                                            )

                                        onSaved()
                                    } catch (_: Exception) {
                                        errorMessage =
                                            "Medication could not be saved."
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text("Save Medication")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(
    title: String,
    message: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title)
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Back to Dashboard")
            }
        }
    }
}