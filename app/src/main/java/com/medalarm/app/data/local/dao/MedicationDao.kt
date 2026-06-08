package com.medalarm.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medalarm.app.data.local.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(med: MedicationEntity): Long

    @Update
    suspend fun update(med: MedicationEntity)

    @Query("UPDATE medications SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): MedicationEntity?

    @Query("SELECT * FROM medications WHERE id = :id")
    fun observeById(id: Long): Flow<MedicationEntity?>

    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<MedicationEntity>>

    /**
     * Atomic stock decrement + clears the lowStockNotified flag if a fresh fill
     * is implied (caller passes negative amount for top-ups). Returns updated rows.
     */
    @Query("""
        UPDATE medications
        SET stockAmount = COALESCE(stockAmount, 0) - :amount,
            lowStockNotified = CASE WHEN :amount < 0 THEN 0 ELSE lowStockNotified END,
            updatedAt = :now
        WHERE id = :id
    """)
    suspend fun adjustStock(id: Long, amount: Float, now: Long): Int

    @Query("UPDATE medications SET lowStockNotified = 1, updatedAt = :now WHERE id = :id")
    suspend fun markLowStockNotified(id: Long, now: Long)
}
