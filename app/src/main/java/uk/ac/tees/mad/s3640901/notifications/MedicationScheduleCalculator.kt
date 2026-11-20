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
        val timeMatch = Regex("""(\d{1,2}):(\d{2})""")
            .find(medication.scheduleValue)
            ?: return null

        val hour = timeMatch.groupValues[1].toIntOrNull()
            ?: return null

        val minute = timeMatch.groupValues[2].toIntOrNull()
            ?: return null

        if (hour !in 0..23 || minute !in 0..59) {
            return null
        }

        return when (
            medication.scheduleType.lowercase(Locale.getDefault())
        ) {
            "daily" -> nextDaily(
                hour = hour,
                minute = minute,
                afterMillis = afterMillis
            )

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
                minute = minute,
                afterMillis = afterMillis
            )

            else -> null
        }
    }

    fun previousDoseTime(
        medication: MedicationEntity,
        beforeMillis: Long = System.currentTimeMillis(),
        notBeforeMillis: Long = Long.MIN_VALUE
    ): Long? {
        val timeMatch = Regex("""(\d{1,2}):(\d{2})""")
            .find(medication.scheduleValue)
            ?: return null

        val hour = timeMatch.groupValues[1].toIntOrNull()
            ?: return null

        val minute = timeMatch.groupValues[2].toIntOrNull()
            ?: return null

        if (hour !in 0..23 || minute !in 0..59) {
            return null
        }

        val previousDose = when (
            medication.scheduleType.lowercase(Locale.getDefault())
        ) {
            "daily" -> previousDaily(
                hour = hour,
                minute = minute,
                beforeMillis = beforeMillis
            )

            "weekly" -> previousWeekly(
                value = medication.scheduleValue,
                hour = hour,
                minute = minute,
                beforeMillis = beforeMillis
            )

            "monthly" -> previousMonthly(
                value = medication.scheduleValue,
                hour = hour,
                minute = minute,
                beforeMillis = beforeMillis
            )

            "one-time",
            "one time" -> previousOneTime(
                value = medication.scheduleValue,
                hour = hour,
                minute = minute,
                beforeMillis = beforeMillis
            )

            else -> null
        }

        // Ignore any dose scheduled before the medicine was added.
        return previousDose?.takeIf {
            it >= notBeforeMillis
        }
    }

    private fun baseCalendar(
        timeMillis: Long,
        hour: Int,
        minute: Int
    ): Calendar {
        return Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
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
        val calendar = baseCalendar(
            timeMillis = afterMillis,
            hour = hour,
            minute = minute
        )

        if (calendar.timeInMillis <= afterMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }

    private fun previousDaily(
        hour: Int,
        minute: Int,
        beforeMillis: Long
    ): Long {
        val calendar = baseCalendar(
            timeMillis = beforeMillis,
            hour = hour,
            minute = minute
        )

        if (calendar.timeInMillis >= beforeMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
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
                value.contains(dayName, ignoreCase = true)
            }
            .values
            .toSet()

        if (selectedDays.isEmpty()) {
            return null
        }

        return (0..7)
            .mapNotNull { offset ->
                val candidate = baseCalendar(
                    timeMillis = afterMillis,
                    hour = hour,
                    minute = minute
                ).apply {
                    add(Calendar.DAY_OF_YEAR, offset)
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

    private fun previousWeekly(
        value: String,
        hour: Int,
        minute: Int,
        beforeMillis: Long
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
                value.contains(dayName, ignoreCase = true)
            }
            .values
            .toSet()

        if (selectedDays.isEmpty()) {
            return null
        }

        return (0..7)
            .mapNotNull { offset ->
                val candidate = baseCalendar(
                    timeMillis = beforeMillis,
                    hour = hour,
                    minute = minute
                ).apply {
                    add(Calendar.DAY_OF_YEAR, -offset)
                }

                if (
                    candidate.get(Calendar.DAY_OF_WEEK) in selectedDays &&
                    candidate.timeInMillis < beforeMillis
                ) {
                    candidate.timeInMillis
                } else {
                    null
                }
            }
            .maxOrNull()
    }

    private fun nextMonthly(
        value: String,
        hour: Int,
        minute: Int,
        afterMillis: Long
    ): Long? {
        val day = Regex("""\d{1,2}""")
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
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, monthOffset)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    val lastDay = getActualMaximum(
                        Calendar.DAY_OF_MONTH
                    )

                    set(
                        Calendar.DAY_OF_MONTH,
                        day.coerceAtMost(lastDay)
                    )
                }

                candidate.timeInMillis.takeIf {
                    it > afterMillis
                }
            }
            .minOrNull()
    }

    private fun previousMonthly(
        value: String,
        hour: Int,
        minute: Int,
        beforeMillis: Long
    ): Long? {
        val day = Regex("""\d{1,2}""")
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
                    timeInMillis = beforeMillis
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, -monthOffset)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    val lastDay = getActualMaximum(
                        Calendar.DAY_OF_MONTH
                    )

                    set(
                        Calendar.DAY_OF_MONTH,
                        day.coerceAtMost(lastDay)
                    )
                }

                candidate.timeInMillis.takeIf {
                    it < beforeMillis
                }
            }
            .maxOrNull()
    }

    private fun nextOneTime(
        value: String,
        hour: Int,
        minute: Int,
        afterMillis: Long
    ): Long? {
        val dateText = value
            .replace(
                Regex("""(?i)\s+at\s+.*$"""),
                ""
            )
            .trim()

        val formats = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "dd MMM yyyy",
            "MMM dd, yyyy",
            "EEE, MMM d, yyyy"
        )

        for (format in formats) {
            val parser = SimpleDateFormat(
                format,
                Locale.US
            ).apply {
                isLenient = false
            }

            val parsedDate = runCatching {
                parser.parse(dateText)
            }.getOrNull() ?: continue

            val doseTime = Calendar.getInstance().apply {
                timeInMillis = parsedDate.time
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            return doseTime.takeIf {
                it > afterMillis
            }
        }

        return null
    }

    private fun previousOneTime(
        value: String,
        hour: Int,
        minute: Int,
        beforeMillis: Long
    ): Long? {
        val dateText = value
            .replace(
                Regex("""(?i)\s+at\s+.*$"""),
                ""
            )
            .trim()

        val formats = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "dd MMM yyyy",
            "MMM dd, yyyy",
            "EEE, MMM d, yyyy"
        )

        for (format in formats) {
            val parser = SimpleDateFormat(
                format,
                Locale.US
            ).apply {
                isLenient = false
            }

            val parsedDate = runCatching {
                parser.parse(dateText)
            }.getOrNull() ?: continue

            val doseTime = Calendar.getInstance().apply {
                timeInMillis = parsedDate.time
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            return doseTime.takeIf {
                it < beforeMillis
            }
        }

        return null
    }
}