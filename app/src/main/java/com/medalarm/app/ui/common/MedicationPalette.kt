package com.medalarm.app.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Curated palette for per-medication accent colors. Calm, medical-friendly hues
 * with enough variety to make a list of 4-6 medications visually distinct.
 *
 * Stored on [com.medalarm.app.domain.model.Medication.colorHex] as `#RRGGBB`.
 */
object MedicationPalette {

    data class Swatch(val hex: String, val color: Color)

    val swatches: List<Swatch> = listOf(
        Swatch("#00696C", Color(0xFF00696C)), // teal (primary)
        Swatch("#3F6FBE", Color(0xFF3F6FBE)), // blue
        Swatch("#5C913B", Color(0xFF5C913B)), // green
        Swatch("#C76A2D", Color(0xFFC76A2D)), // amber
        Swatch("#A33C7A", Color(0xFFA33C7A)), // plum
        Swatch("#B8002E", Color(0xFFB8002E)), // crimson
        Swatch("#8C6D1F", Color(0xFF8C6D1F)), // mustard
        Swatch("#3D5A66", Color(0xFF3D5A66)), // slate
    )

    fun colorFor(hex: String?): Color =
        hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
            ?: swatches.first().color
}

/**
 * Picks a sensible accent for a medication: their stored hex, or the theme primary
 * if they haven't picked one yet.
 */
@Composable
fun resolveMedicationColor(hex: String?): Color =
    hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
        ?: MaterialTheme.colorScheme.primary
