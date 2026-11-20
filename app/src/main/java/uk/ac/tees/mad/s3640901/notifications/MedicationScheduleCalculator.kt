package uk.ac.tees.mad.s3640901.notifications

import uk.ac.tees.mad.s3640901.data.MedicationEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object MedicationScheduleCalculator {

    fun nextDoseTime(
        medication: MedicationEntity,
        afterMillis: Long = System.currentTimeMillis()
    ): Long? {
        val timeMatch = Regex("(\\d{1,2}):(\\d{2})")
            .find(medication.scheduleValue)
            ?: return null

        val hour = timeMatch.groupValues[1].toIntOrNull()
            ?: return null

        val minute = timeMatch.groupValues[2].toIntOrNull()
            ?: return null

        if (hour !in 0..23 || minute !in 0..59) {
            return null
        }

        return when (medication.scheduleType.lowercase(Locale.getDefault())) {
            "daily" -> nextDaily(hour, minute, afterMillis)

            "weekly" -> nextWeekly(
                value = medication.scheduleValue,
                hour = hour,
                minute = minute,
                afterMillis = afterMillis
            )

            "monthly" -> nextMonthly(
                value = medication.scheduleValue,
                hour = hour,
                minute = minute,
                afterMillis = afterMillis
            )

            "one-time",
            "one time" -> nextOneTime(
                value = medication.scheduleValue,
                hour = hour,
                minute = minute
            )

            else -> null
        }
    }

    private fun createBaseCalendar(
        afterMillis: Long,
        hour: Int,
        minute: Int
    ): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = afterMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun nextDaily(
        hour: Int,
        minute: Int,
        afterMillis: Long
    ): Long {
        val calendar = createBaseCalendar(
            afterMillis = afterMillis,
            hour = hour,
            minute = minute
        )

        if (calendar.timeInMillis <= afterMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }

    private fun nextWeekly(
        value: String,
        hour: Int,
        minute: Int,
        afterMillis: Long
    ): Long? {
        val dayMap = mapOf(
            "sun" to Calendar.SUNDAY,
            "mon" to Calendar.MONDAY,
            "tue" to Calendar.TUESDAY,
            "wed" to Calendar.WEDNESDAY,
            "thu" to Calendar.THURSDAY,
            "fri" to Calendar.FRIDAY,
            "sat" to Calendar.SATURDAY
        )

        val selectedDays = dayMap
            .filter { (dayName, _) ->
                value.contains(
                    dayName,
                    ignoreCase = true
                )
            }
            .values
            .toSet()

        if (selectedDays.isEmpty()) {
            return null
        }

        return (0..7)
            .mapNotNull { dayOffset ->
                val candidate = createBaseCalendar(
                    afterMillis = afterMillis,
                    hour = hour,
                    minute = minute
                ).apply {
                    add(
                        Calendar.DAY_OF_YEAR,
                        dayOffset
                    )
                }

                if (
                    candidate.get(Calendar.DAY_OF_WEEK) in selectedDays &&
                    candidate.timeInMillis > afterMillis
                ) {
                    candidate.timeInMillis
                } else {
                    null
                }
            }
            .minOrNull()
    }

    private fun nextMonthly(
        value: String,
        hour: Int,
        minute: Int,
        afterMillis: Long
    ): Long? {
        val day = Regex("\\d{1,2}")
            .find(value.substringBefore("at"))
            ?.value
            ?.toIntOrNull()
            ?: return null

        if (day !in 1..31) {
            return null
        }

        return (0..12)
            .mapNotNull { monthOffset ->
                val candidate = Calendar.getInstance().apply {
                    timeInMillis = afterMillis

                    // Set day to 1 before changing the month.
                    // This prevents invalid dates such as January 31 + one month.
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, monthOffset)

                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    val lastDayOfMonth =
                        getActualMaximum(Calendar.DAY_OF_MONTH)

                    set(
                        Calendar.DAY_OF_MONTH,
                        day.coerceAtMost(lastDayOfMonth)
                    )
                }

                if (candidate.timeInMillis > afterMillis) {
                    candidate.timeInMillis
                } else {
                    null
                }
            }
            .minOrNull()
    }

    private fun nextOneTime(
        value: String,
        hour: Int,
        minute: Int
    ): Long? {
        val dateText = value
            .replace(
                Regex("(?i)\\s+at\\s+.*$"),
                ""
            )
            .trim()

        val supportedFormats = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "dd MMM yyyy",
            "MMM dd, yyyy",
            "EEE, MMM d, yyyy"
        )

        for (format in supportedFormats) {
            val parser = SimpleDateFormat(
                format,
                Locale.US
            ).apply {
                isLenient = false
            }

            val parsedDate = runCatching {
                parser.parse(dateText)
            }.getOrNull() ?: continue

            return Calendar.getInstance().apply {
                timeInMillis = parsedDate.time
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        return null
    }
}