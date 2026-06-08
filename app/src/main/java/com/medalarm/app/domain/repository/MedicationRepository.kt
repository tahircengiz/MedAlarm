package com.medalarm.app.domain.repository

import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.model.Schedule
import kotlinx.coroutines.flow.Flow

interface MedicationRepository {

    fun observeActive(): Flow<List<Medication>>
    fun observeAll(): Flow<List<Medication>>
    fun observe(id: Long): Flow<Medication?>
    suspend fun get(id: Long): Medication?

    /** Inserts a medication and (optionally) its initial schedule atomically. */
    suspend fun add(medication: Medication, schedules: List<Schedule> = emptyList()): Long

    suspend fun update(medication: Medication)

    /** Soft-delete: keeps the row + history but sets isActive = false. */
    suspend fun softDelete(id: Long)

    /**
     * Atomically adjusts stockAmount by [amount] (negative to subtract).
     * If a negative-amount adjustment crosses the threshold, returns true so
     * the caller can post a low-stock notification.
     */
    suspend fun adjustStock(id: Long, amount: Float): StockAdjustResult

    suspend fun markLowStockNotified(id: Long)

    fun observeSchedules(medicationId: Long): Flow<List<Schedule>>
    suspend fun getSchedules(medicationId: Long): List<Schedule>
    suspend fun addSchedule(schedule: Schedule): Long
    suspend fun updateSchedule(schedule: Schedule)
    suspend fun deleteSchedule(scheduleId: Long)

    /** All schedules for medications where isActive = 1. Used by the alarm boot recovery. */
    suspend fun getAllSchedulesForActiveMedications(): List<Schedule>
}

data class StockAdjustResult(
    val newStockAmount: Float?,
    val crossedThreshold: Boolean
)
