package com.medalarm.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.model.MedicationUnit
import java.time.Instant

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("medicationId"),
        Index("scheduleId"),
        Index("scheduledAt"),
        Index("status"),
        // Composite index to keep "today's plan" queries cheap.
        Index(value = ["scheduledAt", "status"], name = "idx_dose_logs_scheduledAt_status")
    ]
)
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val scheduleId: Long,
    val scheduledAt: Instant,
    val status: DoseStatus,
    val actionAt: Instant? = null,
    val snoozeCount: Int = 0,
    val snoozeUntil: Instant? = null,
    val dosageAmountSnapshot: Float,
    val unitSnapshot: MedicationUnit
)
