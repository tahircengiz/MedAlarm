package com.medalarm.app.domain.usecase

import com.medalarm.app.domain.repository.DoseLogRepository
import java.time.Instant
import javax.inject.Inject

class SkipDoseUseCase @Inject constructor(
    private val doseLogRepository: DoseLogRepository
) {
    suspend operator fun invoke(doseLogId: Long, at: Instant = Instant.now()) {
        doseLogRepository.markSkipped(doseLogId, at)
    }
}
