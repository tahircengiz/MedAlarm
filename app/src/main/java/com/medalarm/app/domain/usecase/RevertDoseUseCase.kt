package com.medalarm.app.domain.usecase

import com.medalarm.app.domain.model.DoseStatus
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.MedicationRepository
import javax.inject.Inject

/**
 * Undo: reverts a dose back to PENDING. If the dose had been marked TAKEN and the
 * medication tracks stock, the consumed amount is added back (inverse of
 * [MarkDoseTakenUseCase]). SKIPPED/MISSED carry no stock change, so nothing is added.
 */
class RevertDoseUseCase @Inject constructor(
    private val doseLogRepository: DoseLogRepository,
    private val medicationRepository: MedicationRepository
) {
    suspend operator fun invoke(doseLogId: Long) {
        val log = doseLogRepository.get(doseLogId) ?: return
        if (log.status == DoseStatus.PENDING) return

        if (log.status == DoseStatus.TAKEN) {
            val med = medicationRepository.get(log.medicationId)
            // Only restore stock if tracking is enabled; addStock would otherwise
            // spuriously enable tracking on a medication that doesn't use it.
            if (med?.stockAmount != null) {
                medicationRepository.addStock(log.medicationId, log.dosageAmountSnapshot)
            }
        }
        doseLogRepository.revertToPending(doseLogId)
    }
}
