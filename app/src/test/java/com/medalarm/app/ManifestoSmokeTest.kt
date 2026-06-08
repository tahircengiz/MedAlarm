package com.medalarm.app

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Smoke test that proves the test pipeline runs.
 * Real test suites land alongside their respective use cases.
 */
class ManifestoSmokeTest {
    @Test
    fun `truth library is wired correctly`() {
        assertThat(2 + 2).isEqualTo(4)
    }
}
