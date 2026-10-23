package com.arlindodishi.mathwizard.ui.run

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arlindodishi.mathwizard.data.MetaRepository
import com.arlindodishi.mathwizard.data.RunReward
import com.arlindodishi.mathwizard.engine.AnswerOutcome
import com.arlindodishi.mathwizard.engine.Phase
import com.arlindodishi.mathwizard.engine.RunConfig
import com.arlindodishi.mathwizard.engine.RunEngine
import com.arlindodishi.mathwizard.engine.RunState
import com.arlindodishi.mathwizard.engine.School
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    /** Combo thresholds shift from relics (Combo Crown), for the HUD. */
    val comboShift: Int = 0,
    /** Set once a finished run has been saved. */
    val reward: RunReward? = null,
)

class RunViewModel(
    private val meta: MetaRepository,
    private val school: School = School.ARITHMETIC,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val seeds: () -> Long = { Random.nextLong() },
    /** Injectable so tests can run the endless timer loop in a scope they control. */
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel(scope) {

    private lateinit var engine: RunEngine
    private var seed = 0L

    /** Null until the meta profile (upgrades) has loaded and the first run is created. */
    private val _ui = MutableStateFlow<RunUiState?>(null)
    val ui: StateFlow<RunUiState?> = _ui.asStateFlow()

    // Problem timer: time spent before the last pause plus the running segment.
    private var elapsedBeforePause = 0L
    private var segmentStart = 0L
    private var paused = false

    private var feedbackJob: Job? = null

    init {
        viewModelScope.launch {
            startRun()
            while (isActive) {
                delay(TICK_MILLIS)
                tick()
            }
        }
    }

    private val acceptingInput: Boolean
        get() = _ui.value?.let { it.run.phase == Phase.FIGHTING && it.feedback == null } == true && !paused

    fun choose(choice: String) {
        if (!acceptingInput) return
        onOutcome(engine.answer(choice, elapsed()), chosen = choice)
    }

    fun useHint() {
        if (!acceptingInput || !engine.useHint()) return
        _ui.update { it?.copy(run = engine.state) }
    }

    /** Leaves a cleared floor taking [relicId] (must be on offer), or none when nothing is offered. */
    fun continueToNextFloor(relicId: String? = null) {
        val ui = _ui.value ?: return
        if (ui.run.phase != Phase.FLOOR_CLEARED || ui.feedback != null) return
        engine.nextFloor(relicId)
        startProblemTimer()
        _ui.update {
            it?.copy(
                run = engine.state,
                timeLeftMillis = engine.state.timeLimitMillis,
                comboShift = engine.currentModifiers.comboThresholdShift,
            )
        }
    }

    fun restart() {
        feedbackJob?.cancel()
        viewModelScope.launch { startRun() }
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

    private suspend fun startRun() {
        val bonuses = meta.profile.first().bonuses()
        seed = seeds()
        engine = RunEngine(RunConfig(seed = seed, school = school, bonuses = bonuses))
        startProblemTimer()
        _ui.value = RunUiState(
            run = engine.state,
            timeLeftMillis = engine.state.timeLimitMillis,
            comboShift = engine.currentModifiers.comboThresholdShift,
        )
    }

    private fun tick() {
        if (!acceptingInput) return
        val left = engine.state.timeLimitMillis - elapsed()
        if (left <= 0) {
            onOutcome(engine.timeOut(), chosen = null)
        } else {
            _ui.update { it?.copy(timeLeftMillis = left) }
        }
    }

    private fun onOutcome(outcome: AnswerOutcome, chosen: String?) {
        val state = engine.state
        _ui.update {
            it?.copy(
                run = state,
                feedback = Feedback(outcome, chosen),
                answerCount = it.answerCount + 1,
                timeLeftMillis = if (outcome is AnswerOutcome.Miss && outcome.timedOut) 0 else it.timeLeftMillis,
            )
        }
        if (state.phase == Phase.GAME_OVER) {
            val runSeed = seed
            viewModelScope.launch {
                val reward = meta.recordRun(state, runSeed)
                _ui.update { it?.copy(reward = reward) }
            }
        }
        feedbackJob = viewModelScope.launch {
            delay(FEEDBACK_MILLIS)
            startProblemTimer()
            _ui.update { it?.copy(feedback = null, timeLeftMillis = engine.state.timeLimitMillis) }
        }
    }

    private fun elapsed(): Long = elapsedBeforePause + if (paused) 0 else clock() - segmentStart

    private fun startProblemTimer() {
        elapsedBeforePause = 0
        segmentStart = clock()
    }

    companion object {
        const val TICK_MILLIS = 50L
        const val FEEDBACK_MILLIS = 700L
    }
}
