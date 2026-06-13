package com.medalarm.app.data.repository

import androidx.room.withTransaction
import com.medalarm.app.data.local.MedAlarmDatabase
import com.medalarm.app.data.local.dao.MedicationDao
import com.medalarm.app.data.local.dao.ScheduleDao
import com.medalarm.app.data.local.mapper.toDomain
import com.medalarm.app.data.local.mapper.toEntity
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.model.Schedule
import com.medalarm.app.domain.repository.MedicationRepository
import com.medalarm.app.domain.repository.StockAdjustResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

internal class MedicationRepositoryImpl(
    private val db: MedAlarmDatabase,
    private val medicationDao: MedicationDao,
    private val scheduleDao: ScheduleDao
) : MedicationRepository {

    override fun observeActive(): Flow<List<Medication>> =
        medicationDao.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observeAll(): Flow<List<Medication>> =
        medicationDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observe(id: Long): Flow<Medication?> =
        medicationDao.observeById(id).map { it?.toDomain() }

    override suspend fun get(id: Long): Medication? =
        medicationDao.getById(id)?.toDomain()

    override suspend fun add(medication: Medication, schedules: List<Schedule>): Long =
        db.withTransaction {
            val medId = medicationDao.insert(medication.toEntity())
            schedules.forEach { s ->
                scheduleDao.insert(s.copy(medicationId = medId).toEntity())
            }
            medId
        }

    override suspend fun update(medication: Medication) {
        medicationDao.update(medication.copy(updatedAt = Instant.now()).toEntity())
    }

    override suspend fun softDelete(id: Long) {
        medicationDao.softDelete(id)
    }

    override suspend fun adjustStock(id: Long, amount: Float): StockAdjustResult = db.withTransaction {
        val before = medicationDao.getById(id) ?: return@withTransaction StockAdjustResult(null, false)
        if (before.stockAmount == null) {
            // Stock tracking disabled for this med; no-op.
            return@withTransaction StockAdjustResult(null, false)
        }
        medicationDao.adjustStock(id, amount, Instant.now().toEpochMilli())
        val after = medicationDao.getById(id)
        val newStock = after?.stockAmount
        val threshold = after?.stockThreshold
        val crossed = newStock != null &&
            threshold != null &&
            before.stockAmount > threshold &&
            newStock <= threshold &&
            after.lowStockNotified.not()
        StockAdjustResult(newStockAmount = newStock, crossedThreshold = crossed)
    }

    override suspend fun addStock(id: Long, amount: Float) {
        if (amount <= 0f) return
        val med = medicationDao.getById(id) ?: return
        // Enable tracking if it was off (stockAmount null) by starting from 0.
        val current = med.stockAmount ?: 0f
        medicationDao.update(
            med.copy(
                stockAmount = current + amount,
                lowStockNotified = false,
                updatedAt = Instant.now()
            )
        )
    }

    override suspend fun setStock(id: Long, amount: Float) {
        val med = medicationDao.getById(id) ?: return
        val newAmount = amount.coerceAtLeast(0f)
        // Re-arm low-stock alerts if the corrected amount is back above the threshold.
        val clearNotified = med.stockThreshold == null || newAmount > med.stockThreshold
        medicationDao.update(
            med.copy(
                stockAmount = newAmount,
                lowStockNotified = if (clearNotified) false else med.lowStockNotified,
                updatedAt = Instant.now()
            )
        )
    }

    override suspend fun markLowStockNotified(id: Long) {
        medicationDao.markLowStockNotified(id, Instant.now().toEpochMilli())
    }

    override fun observeSchedules(medicationId: Long): Flow<List<Schedule>> =
        scheduleDao.observeForMedication(medicationId).map { list -> list.map { it.toDomain() } }

    override suspend fun getSchedules(medicationId: Long): List<Schedule> =
        scheduleDao.getForMedication(medicationId).map { it.toDomain() }

    override suspend fun addSchedule(schedule: Schedule): Long =
        scheduleDao.insert(schedule.toEntity())

    override suspend fun updateSchedule(schedule: Schedule) {
        scheduleDao.update(schedule.toEntity())
    }

    override suspend fun deleteSchedule(scheduleId: Long) {
        scheduleDao.delete(scheduleId)
    }

    override suspend fun getAllSchedulesForActiveMedications(): List<Schedule> =
        scheduleDao.getAllForActiveMedications().map { it.toDomain() }
}
