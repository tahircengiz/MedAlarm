package com.medalarm.app.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class DaySwipeTest {

    private val threshold = 100f

    @Test
    fun `swipe left goes to the next day`() {
        // Finger moves right-to-left: negative dx, like turning a page forward.
        assertThat(swipeDayDelta(totalDx = -150f, thresholdPx = threshold)).isEqualTo(1)
    }

    @Test
    fun `swipe right goes to the previous day`() {
        assertThat(swipeDayDelta(totalDx = 150f, thresholdPx = threshold)).isEqualTo(-1)
    }

    @Test
    fun `short drags below the threshold do nothing`() {
        assertThat(swipeDayDelta(totalDx = 99f, thresholdPx = threshold)).isEqualTo(0)
        assertThat(swipeDayDelta(totalDx = -99f, thresholdPx = threshold)).isEqualTo(0)
        assertThat(swipeDayDelta(totalDx = 0f, thresholdPx = threshold)).isEqualTo(0)
    }

    @Test
    fun `a long swipe still moves exactly one day`() {
        assertThat(swipeDayDelta(totalDx = -2000f, thresholdPx = threshold)).isEqualTo(1)
        assertThat(swipeDayDelta(totalDx = 2000f, thresholdPx = threshold)).isEqualTo(-1)
    }
}
