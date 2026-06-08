package com.medalarm.app.data.local.mapper

import com.medalarm.app.data.local.entity.MedicationEntity
import com.medalarm.app.domain.model.Medication

internal fun MedicationEntity.toDomain(): Medication = Medication(
    id = id,
    name = name,
    unit = unit,
    dosageAmount = dosageAmount,
    notes = notes,
    colorHex = colorHex,
    iconKey = iconKey,
    startDate = startDate,
    endDate = endDate,
    stockAmount = stockAmount,
    stockThreshold = stockThreshold,
    lowStockNotified = lowStockNotified,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun Medication.toEntity(): MedicationEntity = MedicationEntity(
    id = id,
    name = name,
    unit = unit,
    dosageAmount = dosageAmount,
    notes = notes,
    colorHex = colorHex,
    iconKey = iconKey,
    startDate = startDate,
    endDate = endDate,
    stockAmount = stockAmount,
    stockThreshold = stockThreshold,
    lowStockNotified = lowStockNotified,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)
