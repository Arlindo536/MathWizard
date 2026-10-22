package com.arlindodishi.mathwizard.ui.run

import com.arlindodishi.mathwizard.data.SpireRecords
import com.arlindodishi.mathwizard.engine.AnswerOutcome
import com.arlindodishi.mathwizard.engine.Phase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RunViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeRecords : SpireRecords {
        override var bestFloor = 0
        override var runsPlayed = 0
        override fun recordRun(floor: Int): Boolean {
            runsPlayed++
            return (floor > bestFloor).also { if (it) bestFloor = floor }
        }
    }

    private val records = FakeRecords()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.newViewModel() =
        RunViewModel(records, clock = { testScheduler.currentTime }, seeds = { 42L }, scope = backgroundScope)

    private fun RunViewModel.answerCorrectly() = choose(ui.value.run.problem.answer)

    private fun RunViewModel.answerWrongly() = choose(ui.value.run.problem.choices.first { it != ui.value.run.problem.answer })

    private fun TestScope.finishFeedback() = advanceTimeBy(RunViewModel.FEEDBACK_MILLIS + 1)

    @Test
    fun correctAnswerShowsFeedbackThenNextProblem() = runTest(dispatcher) {
        val vm = newViewModel()
        val first = vm.ui.value.run.problem
        vm.answerCorrectly()
        val feedback = vm.ui.value.feedback
        assertNotNull(feedback)
        feedback!!
        assertTrue(feedback.outcome is AnswerOutcome.Hit)
        assertEquals(first, feedback.outcome.problem)
        finishFeedback()
        assertNull(vm.ui.value.feedback)
        assertFalse(first == vm.ui.value.run.problem)
    }

    @Test
    fun inputIsIgnoredDuringFeedback() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.answerCorrectly()
        val answered = vm.ui.value.run.stats.answered
        vm.answerCorrectly()
        assertEquals(answered, vm.ui.value.run.stats.answered)
    }

    @Test
    fun timerRunsOutIntoMiss() = runTest(dispatcher) {
        val vm = newViewModel()
        val limit = vm.ui.value.run.timeLimitMillis
        advanceTimeBy(limit / 2)
        assertTrue(vm.ui.value.timeLeftMillis in 1 until limit)
        advanceTimeBy(limit / 2 + RunViewModel.TICK_MILLIS * 2)
        val outcome = vm.ui.value.feedback?.outcome
        assertTrue(outcome is AnswerOutcome.Miss && outcome.timedOut)
        assertTrue(vm.ui.value.run.playerHp < vm.ui.value.run.playerMaxHp)
    }

    @Test
    fun pauseFreezesTimer() = runTest(dispatcher) {
        val vm = newViewModel()
        advanceTimeBy(1_000)
        vm.pause()
        val left = vm.ui.value.timeLeftMillis
        advanceTimeBy(60_000)
        assertNull(vm.ui.value.feedback)
        assertEquals(left, vm.ui.value.timeLeftMillis)
        vm.resume()
        advanceTimeBy(500)
        assertTrue(vm.ui.value.timeLeftMillis < left)
    }

    @Test
    fun clearingFloorWaitsForContinue() = runTest(dispatcher) {
        val vm = newViewModel()
        while (vm.ui.value.run.phase == Phase.FIGHTING) {
            vm.answerCorrectly()
            finishFeedback()
        }
        assertEquals(Phase.FLOOR_CLEARED, vm.ui.value.run.phase)
        advanceTimeBy(60_000)
        assertEquals(Phase.FLOOR_CLEARED, vm.ui.value.run.phase)
        vm.continueToNextFloor()
        assertEquals(2, vm.ui.value.run.floor)
        assertEquals(vm.ui.value.run.timeLimitMillis, vm.ui.value.timeLeftMillis)
    }

    @Test
    fun gameOverRecordsRunOnceAndRestartBeginsFresh() = runTest(dispatcher) {
        val vm = newViewModel()
        while (vm.ui.value.run.phase != Phase.GAME_OVER) {
            vm.answerWrongly()
            finishFeedback()
        }
        assertEquals(1, records.runsPlayed)
        assertEquals(1, records.bestFloor)
        assertTrue(vm.ui.value.newRecord)
        advanceTimeBy(60_000)
        assertEquals(1, records.runsPlayed)

        vm.restart()
        runCurrent()
        assertEquals(Phase.FIGHTING, vm.ui.value.run.phase)
        assertEquals(1, vm.ui.value.run.floor)
        assertEquals(vm.ui.value.run.playerMaxHp, vm.ui.value.run.playerHp)
        assertFalse(vm.ui.value.newRecord)
    }
}
