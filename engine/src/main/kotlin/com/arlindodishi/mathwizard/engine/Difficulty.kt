package com.arlindodishi.mathwizard.engine

import kotlin.math.exp
import kotlin.math.roundToInt

/** Floor-based part of the difficulty: rises fast early, flattens towards 100 (endless). */
object DifficultyCurve {
    private const val SCALE = 30.0

    /** 0 on floor 1, ~26 on floor 10, ~55 on floor 25, ~80 on floor 50, ~96 on floor 100. */
    fun base(floor: Int): Int {
        val f = (floor - 1).coerceAtLeast(0)
        return (MAX_DIFFICULTY * (1 - exp(-f / SCALE))).roundToInt()
    }

    /** Adaptive nudges never take the player below this on a given floor. */
    fun minimum(floor: Int): Int = base(floor) * 3 / 4
}

/**
 * Adaptive part of the difficulty, tracked per school over a rolling window of answers.
 * Fast correct answers push it up, misses (especially consecutive ones) pull it down.
 */
class AdaptiveDifficulty(private val windowSize: Int = DEFAULT_WINDOW) {

    data class Answer(val correct: Boolean, val millis: Long, val targetMillis: Long) {
        val fast: Boolean get() = correct && millis <= targetMillis / 2
    }

    private val history = mutableMapOf<School, ArrayDeque<Answer>>()

    fun record(school: School, answer: Answer) {
        val window = history.getOrPut(school) { ArrayDeque() }
        window.addLast(answer)
        while (window.size > windowSize) window.removeFirst()
    }

    fun recent(school: School): List<Answer> = history[school]?.toList().orEmpty()

    /** Offset in [MIN_OFFSET]..[MAX_OFFSET] derived from the current window. */
    fun offset(school: School): Int {
        val window = history[school] ?: return 0
        var total = window.sumOf { answer ->
            when {
                answer.fast -> FAST_BONUS
                answer.correct -> CORRECT_BONUS
                else -> MISS_PENALTY
            }
        }
        if (window.size >= 2 && window.takeLast(2).none { it.correct }) total += STREAK_MISS_PENALTY
        return total.coerceIn(MIN_OFFSET, MAX_OFFSET)
    }

    fun difficultyFor(school: School, floor: Int): Int =
        (DifficultyCurve.base(floor) + offset(school))
            .coerceIn(DifficultyCurve.minimum(floor), MAX_DIFFICULTY)

    fun reset() = history.clear()

    companion object {
        const val DEFAULT_WINDOW = 8
        const val FAST_BONUS = 3
        const val CORRECT_BONUS = 1
        const val MISS_PENALTY = -4
        const val STREAK_MISS_PENALTY = -4
        const val MIN_OFFSET = -20
        const val MAX_OFFSET = 15
    }
}
