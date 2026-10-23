package com.arlindodishi.mathwizard.ui.run

import com.arlindodishi.mathwizard.engine.AnswerOutcome
import com.arlindodishi.mathwizard.engine.MetaProfile
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
    private val meta = FakeMetaRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.newViewModel(): RunViewModel {
        val vm = RunViewModel(meta, clock = { testScheduler.currentTime }, seeds = { 42L }, scope = backgroundScope)
        runCurrent()
        return vm
    }

    private val RunViewModel.state: RunUiState get() = ui.value!!

    private fun RunViewModel.answerCorrectly() = choose(state.run.problem.answer)

    private fun RunViewModel.answerWrongly() = choose(state.run.problem.choices.first { it != state.run.problem.answer })

    private fun TestScope.finishFeedback() = advanceTimeBy(RunViewModel.FEEDBACK_MILLIS + 1)

    @Test
    fun runStartsOnceProfileLoads() = runTest(dispatcher) {
        val vm = newViewModel()
        assertNotNull(vm.ui.value)
        assertEquals(1, vm.state.run.floor)
    }

    @Test
    fun upgradesApplyToNewRun() = runTest(dispatcher) {
        meta.profile.value = MetaProfile(levels = mapOf("vitality" to 2, "hint" to 1))
        val vm = newViewModel()
        assertEquals(120, vm.state.run.playerMaxHp)
        assertEquals(1, vm.state.run.hintsLeft)
    }

    @Test
    fun correctAnswerShowsFeedbackThenNextProblem() = runTest(dispatcher) {
        val vm = newViewModel()
        val first = vm.state.run.problem
        vm.answerCorrectly()
        val feedback = vm.state.feedback
        assertNotNull(feedback)
        assertTrue(feedback!!.outcome is AnswerOutcome.Hit)
        assertEquals(first, feedback.outcome.problem)
        finishFeedback()
        assertNull(vm.state.feedback)
        assertFalse(first == vm.state.run.problem)
    }

    @Test
    fun inputIsIgnoredDuringFeedback() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.answerCorrectly()
        val answered = vm.state.run.stats.answered
        vm.answerCorrectly()
        assertEquals(answered, vm.state.run.stats.answered)
    }

    @Test
    fun hintRemovesChoices() = runTest(dispatcher) {
        meta.profile.value = MetaProfile(levels = mapOf("hint" to 1))
        val vm = newViewModel()
        vm.useHint()
        assertEquals(2, vm.state.run.problem.choices.size)
        assertEquals(0, vm.state.run.hintsLeft)
    }

    @Test
    fun timerRunsOutIntoMiss() = runTest(dispatcher) {
        val vm = newViewModel()
        val limit = vm.state.run.timeLimitMillis
        advanceTimeBy(limit / 2)
        assertTrue(vm.state.timeLeftMillis in 1 until limit)
        advanceTimeBy(limit / 2 + RunViewModel.TICK_MILLIS * 2)
        val outcome = vm.state.feedback?.outcome
        assertTrue(outcome is AnswerOutcome.Miss && outcome.timedOut)
        assertTrue(vm.state.run.playerHp < vm.state.run.playerMaxHp)
    }

    @Test
    fun pauseFreezesTimer() = runTest(dispatcher) {
        val vm = newViewModel()
        advanceTimeBy(1_000)
        vm.pause()
        val left = vm.state.timeLeftMillis
        advanceTimeBy(60_000)
        assertNull(vm.state.feedback)
        assertEquals(left, vm.state.timeLeftMillis)
        vm.resume()
        advanceTimeBy(500)
        assertTrue(vm.state.timeLeftMillis < left)
    }

    @Test
    fun clearingFloorWaitsForContinue() = runTest(dispatcher) {
        val vm = newViewModel()
        while (vm.state.run.phase == Phase.FIGHTING) {
            vm.answerCorrectly()
            finishFeedback()
        }
        assertEquals(Phase.FLOOR_CLEARED, vm.state.run.phase)
        advanceTimeBy(60_000)
        assertEquals(Phase.FLOOR_CLEARED, vm.state.run.phase)
        vm.continueToNextFloor()
        assertEquals(2, vm.state.run.floor)
        assertEquals(vm.state.run.timeLimitMillis, vm.state.timeLeftMillis)
    }

    @Test
    fun pickingRelicFromOfferAddsItAndAdvances() = runTest(dispatcher) {
        val vm = newViewModel()
        while (vm.state.run.phase == Phase.FIGHTING) {
            vm.answerCorrectly()
            finishFeedback()
        }
        val offer = vm.state.run.relicOffer
        assertEquals(3, offer.size)
        vm.continueToNextFloor(offer[1].id)
        assertEquals(listOf(offer[1]), vm.state.run.relics)
        assertEquals(2, vm.state.run.floor)
    }

    @Test
    fun gameOverRecordsRunOnceWithRewardAndRestartBeginsFresh() = runTest(dispatcher) {
        val vm = newViewModel()
        while (vm.state.run.phase == Phase.FIGHTING) {
            vm.answerCorrectly()
            finishFeedback()
        }
        vm.continueToNextFloor()
        while (vm.state.run.phase != Phase.GAME_OVER) {
            vm.answerWrongly()
            finishFeedback()
        }
        assertEquals(1, meta.recorded.size)
        val reward = vm.state.reward
        assertNotNull(reward)
        assertTrue(reward!!.newRecord)
        assertEquals(2, reward.bestFloor)
        assertTrue(reward.crystals >= 3)
        assertEquals(reward.crystals, meta.profile.value.crystals)
        advanceTimeBy(60_000)
        assertEquals(1, meta.recorded.size)

        vm.restart()
        runCurrent()
        assertEquals(Phase.FIGHTING, vm.state.run.phase)
        assertEquals(1, vm.state.run.floor)
        assertEquals(vm.state.run.playerMaxHp, vm.state.run.playerHp)
        assertNull(vm.state.reward)
    }
}
