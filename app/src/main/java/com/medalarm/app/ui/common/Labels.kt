package com.medalarm.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.medalarm.app.R
import com.medalarm.app.domain.model.MealRelation
import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.model.MedicationUnit

/**
 * Localized labels for domain enums. Screens must never show the raw enum name
 * (e.g. "capsule", "AFTER") — those bypass translation and read as a leak in a
 * non-English UI. Everything funnels through these so the string resources, and
 * their values-tr overrides, are the single source of truth.
 */
@Composable
fun MedicationUnit.localizedLabel(): String = when (this) {
    MedicationUnit.TABLET -> stringResource(R.string.unit_tablet)
    MedicationUnit.CAPSULE -> stringResource(R.string.unit_capsule)
    MedicationUnit.ML -> stringResource(R.string.unit_ml)
    MedicationUnit.MG -> stringResource(R.string.unit_mg)
    MedicationUnit.DROP -> stringResource(R.string.unit_drop)
    MedicationUnit.PUFF -> stringResource(R.string.unit_puff)
    MedicationUnit.SACHET -> stringResource(R.string.unit_sachet)
    MedicationUnit.OTHER -> stringResource(R.string.unit_other)
}

@Composable
fun MealRelation.localizedLabel(): String = when (this) {
    MealRelation.NONE -> stringResource(R.string.meal_none)
    MealRelation.BEFORE -> stringResource(R.string.meal_before)
    MealRelation.AFTER -> stringResource(R.string.meal_after)
    MealRelation.WITH -> stringResource(R.string.meal_with)
}

/** "1 tablet", "2 kapsül" — the dose amount with its localized unit, one format everywhere. */
@Composable
fun Medication.dosageText(): String {
    val amount = if (dosageAmount % 1f == 0f) dosageAmount.toInt().toString()
    else dosageAmount.toString()
    return "$amount ${unit.localizedLabel()}"
}
