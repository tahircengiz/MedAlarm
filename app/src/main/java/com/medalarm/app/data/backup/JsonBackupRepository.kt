package com.medalarm.app.data.backup

import android.content.Context
import android.net.Uri
import com.medalarm.app.BuildConfig
import com.medalarm.app.data.local.MedAlarmDatabase
import com.medalarm.app.data.local.dao.DoseLogDao
import com.medalarm.app.data.local.dao.MedicationDao
import com.medalarm.app.data.local.dao.ScheduleDao
import com.medalarm.app.data.local.entity.DoseLogEntity
import com.medalarm.app.data.local.entity.MedicationEntity
import com.medalarm.app.data.local.entity.ScheduleEntity
import com.medalarm.app.domain.repository.SettingsRepository
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One repository for both export and import of the user's full data set.
 * Pure file I/O happens off the main thread; DB writes use a single
 * Room transaction for atomicity.
 */
@Singleton
class JsonBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: MedAlarmDatabase,
    private val medicationDao: MedicationDao,
    private val scheduleDao: ScheduleDao,
    private val doseLogDao: DoseLogDao,
    private val settingsRepository: SettingsRepository
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    enum class ImportMode { REPLACE, MERGE }

    suspend fun export(target: Uri): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val meds = medicationDao.observeAll().first()
            val schedules = meds.flatMap { scheduleDao.getForMedication(it.id) }
            val now = Instant.now()
            val doseLogs = doseLogDao.observeRange(
                startInclusive = 0L,
                endExclusive = now.toEpochMilli() + ONE_YEAR_MILLIS
            ).first()
            val settings = settingsRepository.get()

            val file = BackupFile(
                exportedAt = now.toString(),
                appVersion = BuildConfig.VERSION_NAME,
                medications = meds.map { it.toDto() },
                schedules = schedules.map { it.toDto() },
                doseLogs = doseLogs.map { it.toDto() },
                settings = SettingsDto(
                    language = settings.language.name,
                    themeMode = settings.themeMode.name,
                    useDynamicColor = settings.useDynamicColor,
                    ttsEnabled = settings.ttsEnabled,
                    defaultSnoozeMinutes = settings.defaultSnoozeMinutes,
                    maxSnoozeCount = settings.maxSnoozeCount,
                    vibrationEnabled = settings.vibrationEnabled,
                    notificationSoundUri = settings.notificationSoundUri,
                    defaultLowStockThreshold = settings.defaultLowStockThreshold
                )
            )

            val bytes = json.encodeToString(BackupFile.serializer(), file).toByteArray(Charsets.UTF_8)
            context.contentResolver.openOutputStream(target, "w")?.use { stream ->
                stream.write(bytes)
                stream.flush()
            } ?: error("Could not open output stream for $target")
        }
    }.onFailure { Timber.e(it, "Backup export failed") }

    suspend fun import(source: Uri, mode: ImportMode): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val text = context.contentResolver.openInputStream(source)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: error("Could not read $source")

            val file = json.decodeFromString(BackupFile.serializer(), text)
            require(file.version == BackupFile.SCHEMA_VERSION) {
                "Unsupported backup version ${file.version}"
            }

            db.withTransaction {
                if (mode == ImportMode.REPLACE) {
                    db.clearAllTables()
                }
                file.medications.forEach { medicationDao.insert(it.toEntity()) }
                file.schedules.forEach { scheduleDao.insert(it.toEntity()) }
                doseLogDao.insertAll(file.doseLogs.map { it.toEntity() })
            }
        }
    }.onFailure { Timber.e(it, "Backup import failed") }

    private companion object {
        // ~1 year window covers the realistic export horizon for personal data.
        val ONE_YEAR_MILLIS = 365L * 24 * 60 * 60 * 1000
    }
}

// ---------- Entity ↔ DTO mappers ----------

private fun MedicationEntity.toDto() = MedicationDto(
    id = id,
    name = name,
    unit = unit,
    dosageAmount = dosageAmount,
    notes = notes,
    colorHex = colorHex,
    iconKey = iconKey,
    startDate = startDate.toString(),
    endDate = endDate?.toString(),
    stockAmount = stockAmount,
    stockThreshold = stockThreshold,
    lowStockNotified = lowStockNotified,
    isActive = isActive,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString()
)

private fun MedicationDto.toEntity() = MedicationEntity(
    id = id,
    name = name,
    unit = unit,
    dosageAmount = dosageAmount,
    notes = notes,
    colorHex = colorHex,
    iconKey = iconKey,
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let(LocalDate::parse),
    stockAmount = stockAmount,
    stockThreshold = stockThreshold,
    lowStockNotified = lowStockNotified,
    isActive = isActive,
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt)
)

private fun ScheduleEntity.toDto() = ScheduleDto(
    id = id,
    medicationId = medicationId,
    type = type,
    times = times,
    intervalHours = intervalHours,
    intervalStartTime = intervalStartTime,
    daysOfWeekBitmask = daysOfWeekBitmask,
    mealRelation = mealRelation
)

private fun ScheduleDto.toEntity() = ScheduleEntity(
    id = id,
    medicationId = medicationId,
    type = type,
    times = times,
    intervalHours = intervalHours,
    intervalStartTime = intervalStartTime,
    daysOfWeekBitmask = daysOfWeekBitmask,
    mealRelation = mealRelation
)

private fun DoseLogEntity.toDto() = DoseLogDto(
    id = id,
    medicationId = medicationId,
    scheduleId = scheduleId,
    scheduledAt = scheduledAt.toString(),
    status = status,
    actionAt = actionAt?.toString(),
    snoozeCount = snoozeCount,
    snoozeUntil = snoozeUntil?.toString(),
    dosageAmountSnapshot = dosageAmountSnapshot,
    unitSnapshot = unitSnapshot
)

private fun DoseLogDto.toEntity() = DoseLogEntity(
    id = id,
    medicationId = medicationId,
    scheduleId = scheduleId,
    scheduledAt = Instant.parse(scheduledAt),
    status = status,
    actionAt = actionAt?.let(Instant::parse),
    snoozeCount = snoozeCount,
    snoozeUntil = snoozeUntil?.let(Instant::parse),
    dosageAmountSnapshot = dosageAmountSnapshot,
    unitSnapshot = unitSnapshot
)
