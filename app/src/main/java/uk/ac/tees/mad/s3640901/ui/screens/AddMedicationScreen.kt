@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package uk.ac.tees.mad.s3640901.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.tees.mad.s3640901.api.MedicineRepository
import uk.ac.tees.mad.s3640901.api.MedicineSuggestion
import uk.ac.tees.mad.s3640901.data.MedicationDao
import uk.ac.tees.mad.s3640901.data.MedicationEntity
import uk.ac.tees.mad.s3640901.notifications.MedicationReminderScheduler
import java.io.File
import java.util.Calendar

private enum class ScheduleType(
    val label: String
) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    ONE_TIME("One-time")
}

@Composable
fun AddMedicationScreen(
    medicationDao: MedicationDao,
    medication: MedicationEntity?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val medicineRepository = remember {
        MedicineRepository()
    }

    val isEditing = medication != null

    var name by rememberSaveable(medication?.id) {
        mutableStateOf(medication?.name.orEmpty())
    }

    var dosage by rememberSaveable(medication?.id) {
        mutableStateOf(medication?.dosage.orEmpty())
    }

    var instructions by rememberSaveable(medication?.id) {
        mutableStateOf(medication?.instructions.orEmpty())
    }

    var selectedTime by rememberSaveable(medication?.id) {
        mutableStateOf(
            extractTime(medication?.scheduleValue.orEmpty())
        )
    }

    var selectedDate by rememberSaveable(medication?.id) {
        mutableStateOf(
            extractDate(medication?.scheduleValue.orEmpty())
        )
    }

    var monthlyDay by rememberSaveable(medication?.id) {
        mutableStateOf(
            extractMonthlyDay(
                medication?.scheduleValue.orEmpty()
            )
        )
    }

    var selectedDays by remember(medication?.id) {
        mutableStateOf(
            extractDays(
                medication?.scheduleValue.orEmpty()
            )
        )
    }

    var scheduleType by rememberSaveable(medication?.id) {
        mutableStateOf(
            scheduleTypeFromString(
                medication?.scheduleType
            )
        )
    }

    var scheduleMenuExpanded by remember {
        mutableStateOf(false)
    }

    var photoPath by rememberSaveable(medication?.id) {
        mutableStateOf(medication?.photoPath)
    }

    var errorMessage by rememberSaveable {
        mutableStateOf("")
    }

    var isSaving by rememberSaveable {
        mutableStateOf(false)
    }

    var isSearching by remember {
        mutableStateOf(false)
    }

    var suggestionMessage by remember {
        mutableStateOf("")
    }

    var suggestions by remember {
        mutableStateOf<List<MedicineSuggestion>>(
            emptyList()
        )
    }

    var selectedMedicineName by rememberSaveable {
        mutableStateOf(
            medication?.genericName.orEmpty()
        )
    }

    var cameraInProgress by rememberSaveable {
        mutableStateOf(false)
    }

    val takePictureLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap: Bitmap? ->

            cameraInProgress = false

            if (bitmap == null) {
                errorMessage = "Camera capture was cancelled."
            } else {
                scope.launch {
                    val savedPath = withContext(Dispatchers.IO) {
                        saveMedicinePhoto(
                            context = context,
                            bitmap = bitmap
                        )
                    }

                    if (savedPath == null) {
                        errorMessage =
                            "The photo could not be saved."
                    } else {
                        photoPath = savedPath
                        errorMessage = ""
                    }
                }
            }
        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                cameraInProgress = true

                runCatching {
                    takePictureLauncher.launch(null)
                }.onFailure {
                    cameraInProgress = false
                    errorMessage = "Unable to open the camera."
                }
            } else {
                errorMessage =
                    "Camera permission is required."
            }
        }

    fun openCamera() {
        val permissionGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            cameraInProgress = true

            runCatching {
                takePictureLauncher.launch(null)
            }.onFailure {
                cameraInProgress = false
                errorMessage = "Unable to open the camera."
            }
        } else {
            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    LaunchedEffect(name) {
        val prefix = name.trim()

        suggestions = emptyList()
        suggestionMessage = ""

        if (prefix.length < 2) {
            isSearching = false
            return@LaunchedEffect
        }

        delay(500)

        isSearching = true

        medicineRepository
            .suggestMedicine(prefix)
            .onSuccess { result ->
                suggestions = result

                if (result.isEmpty()) {
                    suggestionMessage =
                        "No matching medicine found. " +
                                "You can enter the name manually."
                }
            }
            .onFailure {
                suggestions = emptyList()
                suggestionMessage =
                    "Unable to connect to OpenFDA. " +
                            "Check your internet connection."
            }

        isSearching = false
    }

    fun showTimePicker() {
        val now = Calendar.getInstance()

        TimePickerDialog(
            context,
            { _, hour, minute ->
                selectedTime =
                    "%02d:%02d".format(hour, minute)
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
                    Text(
                        if (isEditing) {
                            "Edit Medication"
                        } else {
                            "Add Medication"
                        }
                    )
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
            Spacer(
                modifier = Modifier.height(4.dp)
            )

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            suggestions.forEach { suggestion ->
                MedicineSuggestionCard(
                    suggestion = suggestion,
                    onSelected = {
                        name = suggestion.name
                        selectedMedicineName =
                            suggestion.name
                        suggestions = emptyList()
                        suggestionMessage = ""
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
                    Text("Instructions")
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
                enabled = !isSaving && !cameraInProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        cameraInProgress ->
                            "Opening camera..."

                        photoPath == null ->
                            "Take medicine photo"

                        else ->
                            "Retake medicine photo"
                    }
                )
            }

            photoPath?.let { path ->
                val bitmap = remember(path) {
                    BitmapFactory
                        .decodeFile(path)
                        ?.asImageBitmap()
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription =
                            "Medicine photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
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
                        .horizontalScroll(
                            rememberScrollState()
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
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
                        monthlyDay =
                            it.filter { character ->
                                character.isDigit()
                            }
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

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Button(
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

                        val updatedMedication =
                            MedicationEntity(
                                id = medication?.id ?: 0L,
                                name = name.trim(),
                                dosage = dosage.trim(),
                                instructions = instructions.trim(),
                                scheduleType = scheduleType.label,
                                scheduleValue = scheduleValue,
                                isActive = true,
                                photoPath = photoPath,
                                createdAt =
                                    medication?.createdAt
                                        ?: System.currentTimeMillis(),
                                genericName =
                                    selectedMedicineName
                                        .ifBlank { null },
                                brandName =
                                    medication?.brandName
                            )

                        isSaving = true
                        errorMessage = ""

                        scope.launch {
                            try {
                                val savedMedication =
                                    withContext(Dispatchers.IO) {
                                        if (medication == null) {
                                            val newId =
                                                medicationDao
                                                    .insertMedication(
                                                        updatedMedication
                                                    )

                                            updatedMedication.copy(
                                                id = newId
                                            )
                                        } else {
                                            medicationDao
                                                .updateMedication(
                                                    updatedMedication
                                                )

                                            updatedMedication
                                        }
                                    }

                                if (medication != null) {
                                    MedicationReminderScheduler
                                        .cancelAllForMedication(
                                            context = context,
                                            medication = medication
                                        )
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
                    },
                    enabled = !isSaving,
                    modifier = Modifier.width(170.dp)
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

            Spacer(
                modifier = Modifier.height(24.dp)
            )
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
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = suggestion.name,
                modifier = Modifier.weight(1f)
            )

            OutlinedButton(
                onClick = onSelected
            ) {
                Text("Use")
            }
        }
    }
}

private fun saveMedicinePhoto(
    context: Context,
    bitmap: Bitmap
): String? {
    return runCatching {
        val directory = File(
            context.filesDir,
            "medicine_photos"
        )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val photoFile = File(
            directory,
            "medicine_${System.currentTimeMillis()}.jpg"
        )

        photoFile.outputStream().use { outputStream ->
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                92,
                outputStream
            )
        }

        photoFile.absolutePath
    }.getOrNull()
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
    if (name.trim().isBlank()) {
        return "Enter a medication name."
    }

    if (dosage.trim().isBlank()) {
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
        return "Select a date."
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
        ScheduleType.DAILY -> {
            "Every day at $selectedTime"
        }

        ScheduleType.WEEKLY -> {
            val orderedDays = listOf(
                "Mon",
                "Tue",
                "Wed",
                "Thu",
                "Fri",
                "Sat",
                "Sun"
            )

            val days = orderedDays
                .filter { it in selectedDays }
                .joinToString(", ")

            "$days at $selectedTime"
        }

        ScheduleType.MONTHLY -> {
            "Day $monthlyDay at $selectedTime"
        }

        ScheduleType.ONE_TIME -> {
            "$selectedDate at $selectedTime"
        }
    }
}

private fun scheduleTypeFromString(
    value: String?
): ScheduleType {
    return when (value?.lowercase()) {
        "weekly" -> ScheduleType.WEEKLY
        "monthly" -> ScheduleType.MONTHLY
        "one-time",
        "one time" -> ScheduleType.ONE_TIME
        else -> ScheduleType.DAILY
    }
}

private fun extractTime(value: String): String {
    val match = Regex(
        """(\d{1,2}):(\d{2})"""
    ).find(value)

    return match?.value.orEmpty()
}

private fun extractDate(value: String): String {
    val match = Regex(
        """\d{4}-\d{2}-\d{2}"""
    ).find(value)

    return match?.value.orEmpty()
}

private fun extractMonthlyDay(value: String): String {
    val match = Regex(
        """(?i)day\s+(\d{1,2})"""
    ).find(value)

    return match?.groupValues?.getOrNull(1).orEmpty()
}

private fun extractDays(value: String): Set<String> {
    val validDays = setOf(
        "Mon",
        "Tue",
        "Wed",
        "Thu",
        "Fri",
        "Sat",
        "Sun"
    )

    return validDays.filterTo(mutableSetOf()) { day ->
        value.contains(
            day,
            ignoreCase = true
        )
    }
}