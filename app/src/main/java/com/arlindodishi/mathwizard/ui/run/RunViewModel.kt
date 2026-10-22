package com.arlindodishi.mathwizard.ui.run

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arlindodishi.mathwizard.data.SpireRecords
import com.arlindodishi.mathwizard.engine.AnswerOutcome
import com.arlindodishi.mathwizard.engine.Phase
import com.arlindodishi.mathwizard.engine.RunConfig
import com.arlindodishi.mathwizard.engine.RunEngine
import com.arlindodishi.mathwizard.engine.RunState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Shown briefly after an answer; [chosen] is null when the timer ran out. */
data class Feedback(val outcome: AnswerOutcome, val chosen: String?)

data class RunUiState(
    val run: RunState,
    val timeLeftMillis: Long,
    val feedback: Feedback? = null,
    /** Bumped on every answer so the UI can replay hit/shake animations. */
    val answerCount: Int = 0,
    val bestFloor: Int = 0,
    val newRecord: Boolean = false,
)

class RunViewModel(
    private val records: SpireRecords,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val seeds: () -> Long = { Random.nextLong() },
    /** Injectable so tests can run the endless timer loop in a scope they control. */
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel(scope) {

    private var engine = RunEngine(RunConfig(seed = seeds()))

    private val _ui = MutableStateFlow(freshState())
    val ui: StateFlow<RunUiState> = _ui.asStateFlow()

    // Problem timer: time spent before the last pause plus the running segment.
    private var elapsedBeforePause = 0L
    private var segmentStart = clock()
    private var paused = false

    private var feedbackJob: Job? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                tick()
            }
        }
    }

    private val acceptingInput: Boolean
        get() = _ui.value.run.phase == Phase.FIGHTING && _ui.value.feedback == null && !paused

    fun choose(choice: String) {
        if (!acceptingInput) return
        onOutcome(engine.answer(choice, elapsed()), chosen = choice)
    }

    fun continueToNextFloor() {
        if (_ui.value.run.phase != Phase.FLOOR_CLEARED || _ui.value.feedback != null) return
        engine.nextFloor()
        startProblemTimer()
        _ui.update { it.copy(run = engine.state, timeLeftMillis = engine.state.timeLimitMillis) }
    }

    fun restart() {
        feedbackJob?.cancel()
        engine = RunEngine(RunConfig(seed = seeds()))
        startProblemTimer()
        _ui.value = freshState()
    }

    fun pause() {
        if (paused) return
        elapsedBeforePause += clock() - segmentStart
        paused = true
    }

    fun resume() {
        if (!paused) return
        segmentStart = clock()
        paused = false
    }

    private fun tick() {
        if (!acceptingInput) return
        val left = engine.state.timeLimitMillis - elapsed()
        if (left <= 0) {
            onOutcome(engine.timeOut(), chosen = null)
        } else {
            _ui.update { it.copy(timeLeftMillis = left) }
        }
    }

    private fun onOutcome(outcome: AnswerOutcome, chosen: String?) {
        val state = engine.state
        var newRecord = false
        if (state.phase == Phase.GAME_OVER) newRecord = records.recordRun(state.floor)
        _ui.update {
            it.copy(
                run = state,
                feedback = Feedback(outcome, chosen),
                answerCount = it.answerCount + 1,
                timeLeftMillis = if (outcome is AnswerOutcome.Miss && outcome.timedOut) 0 else it.timeLeftMillis,
                bestFloor = records.bestFloor,
                newRecord = newRecord,
            )
        }
        feedbackJob = viewModelScope.launch {
            delay(FEEDBACK_MILLIS)
            startProblemTimer()
            _ui.update { it.copy(feedback = null, timeLeftMillis = engine.state.timeLimitMillis) }
        }
    }

    private fun elapsed(): Long = elapsedBeforePause + if (paused) 0 else clock() - segmentStart

    private fun startProblemTimer() {
        elapsedBeforePause = 0
        segmentStart = clock()
    }

    private fun freshState() = RunUiState(
        run = engine.state,
        timeLeftMillis = engine.state.timeLimitMillis,
        bestFloor = records.bestFloor,
    )

    companion object {
        const val TICK_MILLIS = 50L
        const val FEEDBACK_MILLIS = 700L
    }
}
