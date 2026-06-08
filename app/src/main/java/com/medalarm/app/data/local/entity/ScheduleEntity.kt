package com.medalarm.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.medalarm.app.domain.model.MealRelation
import com.medalarm.app.domain.model.ScheduleType

@Entity(
    tableName = "schedules",
    foreignKeys = [ForeignKey(
        entity = MedicationEntity::class,
        parentColumns = ["id"],
        childColumns = ["medicationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("medicationId")]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val type: ScheduleType,
    /** JSON list of HH:mm strings — see [Converters]. */
    val times: List<String> = emptyList(),
    val intervalHours: Int? = null,
    val intervalStartTime: String? = null,
    val daysOfWeekBitmask: Int = 0,
    val mealRelation: MealRelation = MealRelation.NONE
)
