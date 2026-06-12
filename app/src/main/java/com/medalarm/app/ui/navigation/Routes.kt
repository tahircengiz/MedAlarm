package com.medalarm.app.ui.navigation

/**
 * Single source of truth for navigation routes. Keeping them as constants
 * (not enum) so they compose cleanly with NavHost's string-based API.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val SETTINGS_SYSTEM_STATUS = "settings/system_status"
    const val SETTINGS_BACKUP = "settings/backup"
    const val MEDICATIONS = "medications"
    const val STOCK = "stock"
    const val ADD_MEDICATION = "medication/add"
    const val EDIT_MEDICATION = "medication/edit/{medicationId}"
    const val MEDICATION_DETAIL = "medication/{medicationId}"
    const val HISTORY = "history"

    /** SavedStateHandle key matching the path placeholder. */
    const val MEDICATION_ID_KEY = "medicationId"

    fun editMedication(id: Long) = "medication/edit/$id"
    fun medicationDetail(id: Long) = "medication/$id"
}
