package com.medalarm.app.data.local.mapper

import com.medalarm.app.data.local.entity.DoseLogEntity
import com.medalarm.app.domain.model.DoseLog

internal fun DoseLogEntity.toDomain(): DoseLog = DoseLog(
    id = id,
    medicationId = medicationId,
    scheduleId = scheduleId,
    scheduledAt = scheduledAt,
    status = status,
    actionAt = actionAt,
    snoozeCount = snoozeCount,
    snoozeUntil = snoozeUntil,
    dosageAmountSnapshot = dosageAmountSnapshot,
    unitSnapshot = unitSnapshot
)

internal fun DoseLog.toEntity(): DoseLogEntity = DoseLogEntity(
    id = id,
    medicationId = medicationId,
    scheduleId = scheduleId,
    scheduledAt = scheduledAt,
    status = status,
    actionAt = actionAt,
    snoozeCount = snoozeCount,
    snoozeUntil = snoozeUntil,
    dosageAmountSnapshot = dosageAmountSnapshot,
    unitSnapshot = unitSnapshot
)
