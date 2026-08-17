package com.crossplatform.sdk.presentation.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class UpiTimerViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts at TIMER_TOTAL (300 seconds)`() {
        val viewModel = UpiTimerViewModel()

        assertEquals(300, viewModel.timeRemaining.value)
        assertEquals(300, UpiTimerViewModel.TIMER_TOTAL)
    }

    @Test
    fun `counts down by one every second once virtual time advances`() = runTest(dispatcher) {
        val viewModel = UpiTimerViewModel()

        advanceTimeBy(5_500) // 5.5 "seconds" of virtual time

        assertEquals(295, viewModel.timeRemaining.value)
    }

    @Test
    fun `stopTimer freezes the countdown even as time continues to advance`() = runTest(dispatcher) {
        val viewModel = UpiTimerViewModel()
        advanceTimeBy(3_000)
        val remainingAtStop = viewModel.timeRemaining.value

        viewModel.stopTimer()
        advanceTimeBy(10_000)

        assertEquals(remainingAtStop, viewModel.timeRemaining.value)
    }

    @Test
    fun `startTimer after stopTimer resumes counting down from the current value`() = runTest(dispatcher) {
        val viewModel = UpiTimerViewModel()
        advanceTimeBy(3_000) // -> 297
        viewModel.stopTimer()

        viewModel.startTimer()
        advanceTimeBy(2_000)

        assertEquals(295, viewModel.timeRemaining.value)
    }

    @Test
    fun `counts all the way down to zero and stops there, never going negative`() = runTest(dispatcher) {
        val viewModel = UpiTimerViewModel()

        advanceTimeBy(301_000) // more than the full 300s

        assertEquals(0, viewModel.timeRemaining.value)
    }
}
