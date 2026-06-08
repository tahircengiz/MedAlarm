package com.medalarm.app.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * A medication the user wants to be reminded about.
 *
 * Pure Kotlin — no Room/Android dependencies. Mapped from [MedicationEntity]
 * by the data layer.
 */
data class Medication(
    val id: Long = 0,
    val name: String,
    val unit: MedicationUnit,
    /** Amount per single dose, in [unit]s. E.g. 1.0 tablet, 5.0 ml. */
    val dosageAmount: Float,
    val notes: String? = null,
    /** Hex string `#RRGGBB`, used as accent for the medication card. Null = default. */
    val colorHex: String? = null,
    /** Key into a fixed icon set bundled with the app. Null = default pill icon. */
    val iconKey: String? = null,

    /** Treatment start date (inclusive). */
    val startDate: LocalDate,
    /** Treatment end date (inclusive). Null = open-ended (chronic medication). */
    val endDate: LocalDate? = null,

    /** Current stock in [unit]s. Null = stock tracking disabled for this medication. */
    val stockAmount: Float? = null,
    /** Notify when [stockAmount] falls to or below this value. Null = no notifications. */
    val stockThreshold: Float? = null,
    /** True once the low-stock notification has been posted, to avoid nagging. Reset
     *  when [stockAmount] is raised above [stockThreshold] (e.g. user refills). */
    val lowStockNotified: Boolean = false,

    /** Soft-delete + pause. Inactive medications keep their history but no new
     *  doses are scheduled. */
    val isActive: Boolean = true,

    val createdAt: Instant,
    val updatedAt: Instant
)
