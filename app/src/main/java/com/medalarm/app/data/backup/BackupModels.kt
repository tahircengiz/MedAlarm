package com.medalarm.app.data.backup

import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.model.MealRelation
import com.medalarm.app.domain.model.MedicationUnit
import com.medalarm.app.domain.model.ScheduleType
import kotlinx.serialization.Serializable

/**
 * Wire format for JSON backups. Versioned for forward compatibility.
 * See docs/DATA_MODEL.md for the canonical schema.
 *
 * All times are ISO-8601 strings (Instant.toString() / LocalDate.toString())
 * for human readability and tooling-friendliness.
 */
@Serializable
data class BackupFile(
    val version: Int = SCHEMA_VERSION,
    val exportedAt: String,
    val appVersion: String,
    val medications: List<MedicationDto>,
    val schedules: List<ScheduleDto>,
    val doseLogs: List<DoseLogDto>,
    val settings: SettingsDto
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class MedicationDto(
    val id: Long,
    val name: String,
    val unit: MedicationUnit,
    val dosageAmount: Float,
    val notes: String? = null,
    val colorHex: String? = null,
    val iconKey: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val stockAmount: Float? = null,
    val stockThreshold: Float? = null,
    val lowStockNotified: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ScheduleDto(
    val id: Long,
    val medicationId: Long,
    val type: ScheduleType,
    val times: List<String> = emptyList(),
    val intervalHours: Int? = null,
    val intervalStartTime: String? = null,
    val daysOfWeekBitmask: Int = 0,
    val mealRelation: MealRelation = MealRelation.NONE
)

@Serializable
data class DoseLogDto(
    val id: Long,
    val medicationId: Long,
    val scheduleId: Long,
    val scheduledAt: String,
    val status: DoseStatus,
    val actionAt: String? = null,
    val snoozeCount: Int = 0,
    val snoozeUntil: String? = null,
    val dosageAmountSnapshot: Float,
    val unitSnapshot: MedicationUnit
)

@Serializable
data class SettingsDto(
    val language: String = "SYSTEM",
    val themeMode: String = "SYSTEM",
    val useDynamicColor: Boolean = true,
    val ttsEnabled: Boolean = false,
    val defaultSnoozeMinutes: Int = 15,
    val maxSnoozeCount: Int = 3,
    val vibrationEnabled: Boolean = true,
    val notificationSoundUri: String? = null,
    val defaultLowStockThreshold: Float = 5f,
    val swipeRightAction: String = "TAKEN",
    val swipeLeftAction: String = "SNOOZE",
    val largeTextMode: Boolean = false
)
