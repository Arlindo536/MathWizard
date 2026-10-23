package com.arlindodishi.mathwizard.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arlindodishi.mathwizard.engine.PracticeLevel
import com.arlindodishi.mathwizard.engine.Problem
import com.arlindodishi.mathwizard.engine.Problems
import com.arlindodishi.mathwizard.engine.School
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class PracticeUiState(
    val problem: Problem,
    /** The tapped choice while feedback is shown; null while answering. */
    val chosen: String? = null,
    val correct: Int = 0,
    val answered: Int = 0,
    val streak: Int = 0,
) {
    val showingAnswer: Boolean get() = chosen != null
    val wasCorrect: Boolean get() = chosen != null && problem.isCorrect(chosen)
}

/**
 * Practice: endless problems of one school at one fixed level, no HP and no timer. A correct
 * answer moves on by itself; after a wrong one the correct answer stays up until "Next".
 */
class PracticeViewModel(
    private val school: School,
    private val level: PracticeLevel,
    private val seeds: () -> Long = { Random.nextLong() },
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel(scope) {

    private val _ui = MutableStateFlow(PracticeUiState(nextProblem()))
    val ui: StateFlow<PracticeUiState> = _ui.asStateFlow()

    fun choose(choice: String) {
        val s = _ui.value
        if (s.showingAnswer) return
        val right = s.problem.isCorrect(choice)
        _ui.value = s.copy(
            chosen = choice,
            answered = s.answered + 1,
            correct = s.correct + if (right) 1 else 0,
            streak = if (right) s.streak + 1 else 0,
        )
        if (right) {
            viewModelScope.launch {
                delay(AUTO_NEXT_MILLIS)
                next()
            }
        }
    }

    fun next() {
        if (!_ui.value.showingAnswer) return
        _ui.update { it.copy(problem = nextProblem(), chosen = null) }
    }

    private fun nextProblem(): Problem = Problems.generate(school, level.difficulty, seeds())

    companion object {
        const val AUTO_NEXT_MILLIS = 700L
    }
}
