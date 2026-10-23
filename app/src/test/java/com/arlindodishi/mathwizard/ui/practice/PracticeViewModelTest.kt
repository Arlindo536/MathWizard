package com.arlindodishi.mathwizard.ui.practice

import com.arlindodishi.mathwizard.engine.PracticeLevel
import com.arlindodishi.mathwizard.engine.School
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private var seed = 0L

    private fun TestScope.vm(level: PracticeLevel = PracticeLevel.MEDIUM) =
        PracticeViewModel(School.FRACTIONS, level, seeds = { seed++ }, scope = backgroundScope)

    @Test
    fun problemsComeFromTheChosenSchoolAndLevel() = runTest(dispatcher) {
        val p = vm(PracticeLevel.HARD).ui.value.problem
        assertEquals(School.FRACTIONS, p.school)
        assertEquals(PracticeLevel.HARD.difficulty, p.difficulty)
    }

    @Test
    fun correctAnswerCountsAndMovesOnByItself() = runTest(dispatcher) {
        val vm = vm()
        val first = vm.ui.value.problem
        vm.choose(first.answer)
        assertTrue(vm.ui.value.wasCorrect)
        assertEquals(1, vm.ui.value.correct)
        assertEquals(1, vm.ui.value.streak)
        advanceTimeBy(PracticeViewModel.AUTO_NEXT_MILLIS + 1)
        assertFalse(vm.ui.value.showingAnswer)
        assertNotEquals(first, vm.ui.value.problem)
    }

    @Test
    fun wrongAnswerWaitsForNextAndResetsStreak() = runTest(dispatcher) {
        val vm = vm()
        vm.choose(vm.ui.value.problem.answer)
        advanceTimeBy(PracticeViewModel.AUTO_NEXT_MILLIS + 1)
        val p = vm.ui.value.problem
        vm.choose(p.choices.first { it != p.answer })
        advanceTimeBy(60_000)
        assertTrue("stays on the answer until Next", vm.ui.value.showingAnswer)
        assertEquals(0, vm.ui.value.streak)
        assertEquals(1, vm.ui.value.correct)
        assertEquals(2, vm.ui.value.answered)
        vm.choose(p.answer)
        assertEquals("no second answer on the same problem", 2, vm.ui.value.answered)
        vm.next()
        assertFalse(vm.ui.value.showingAnswer)
    }
}
