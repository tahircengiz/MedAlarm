package com.medalarm.app.domain.usecase

import com.medalarm.app.domain.model.Medication
import com.medalarm.app.domain.repository.DoseLogRepository
import com.medalarm.app.domain.repository.MedicationRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Marks a dose as TAKEN, atomically decrements stock by the dose amount, and
 * tells the caller whether a low-stock notification should be posted.
 *
 * Caller (notification action receiver) is responsible for showing the
 * notification — keeping that I/O out of the use case lets us unit test it.
 */
class MarkDoseTakenUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val doseLogRepository: DoseLogRepository
) {

    data class Result(val crossedThreshold: Boolean, val medication: Medication?)

    suspend operator fun invoke(doseLogId: Long, at: Instant = Instant.now()): Result {
        val log = doseLogRepository.get(doseLogId) ?: return Result(false, null)
        doseLogRepository.markTaken(doseLogId, at)
        val stockResult = medicationRepository.adjustStock(log.medicationId, amount = log.dosageAmountSnapshot)
        val med = medicationRepository.get(log.medicationId)
        if (stockResult.crossedThreshold) {
            medicationRepository.markLowStockNotified(log.medicationId)
        }
        return Result(crossedThreshold = stockResult.crossedThreshold, medication = med)
    }
}
