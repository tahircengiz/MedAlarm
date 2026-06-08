package com.medalarm.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medalarm.app.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(schedule: ScheduleEntity): Long

    @Update
    suspend fun update(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: Long): ScheduleEntity?

    @Query("SELECT * FROM schedules WHERE medicationId = :medId")
    suspend fun getForMedication(medId: Long): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE medicationId = :medId")
    fun observeForMedication(medId: Long): Flow<List<ScheduleEntity>>

    @Query("""
        SELECT s.* FROM schedules s
        INNER JOIN medications m ON m.id = s.medicationId
        WHERE m.isActive = 1
    """)
    suspend fun getAllForActiveMedications(): List<ScheduleEntity>
}
