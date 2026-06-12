package com.medalarm.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.medalarm.app.domain.model.MedicationUnit
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: MedicationUnit,
    val dosageAmount: Float,
    val notes: String? = null,
    val colorHex: String? = null,
    val iconKey: String? = null,
    val photoPath: String? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val stockAmount: Float? = null,
    val stockThreshold: Float? = null,
    val lowStockNotified: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)
