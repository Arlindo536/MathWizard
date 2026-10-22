package com.arlindodishi.mathwizard.engine

import kotlin.math.roundToInt
import kotlin.random.Random

data class RunConfig(
    val seed: Long,
    val school: School = School.ARITHMETIC,
    /** Longer timers for players who want less pressure. */
    val relaxed: Boolean = false,
    val playerMaxHp: Int = Balance.PLAYER_MAX_HP,
)

enum class Phase { FIGHTING, FLOOR_CLEARED, GAME_OVER }

data class RunStats(
    val answered: Int = 0,
    val correct: Int = 0,
    val fastAnswers: Int = 0,
    val bestStreak: Int = 0,
    val floorsCleared: Int = 0,
    val damageDealt: Int = 0,
) {
    val accuracy: Double get() = if (answered == 0) 0.0 else correct.toDouble() / answered
}

data class RunState(
    val floor: Int,
    val playerHp: Int,
    val playerMaxHp: Int,
    val enemy: Enemy,
    val problem: Problem,
    val timeLimitMillis: Long,
    val streak: Int,
    val phase: Phase,
    val stats: RunStats,
)

sealed interface AnswerOutcome {
    /** The answered problem, for showing feedback while [RunState.problem] has moved on. */
    val problem: Problem

    data class Hit(
        override val problem: Problem,
        val damage: Int,
        val fast: Boolean,
        val comboMultiplier: Double,
        val enemyDefeated: Boolean,
    ) : AnswerOutcome

    data class Miss(
        override val problem: Problem,
        val damageTaken: Int,
        val timedOut: Boolean,
        val playerDefeated: Boolean,
    ) : AnswerOutcome
}

/** Tunable numbers for the run loop, kept together for balancing passes. */
object Balance {
    const val PLAYER_MAX_HP = 100
    const val SPELL_DAMAGE = 25
    const val FLOOR_HEAL = 15

    /** Base hits to defeat an enemy: 5 early, rising to 7 (speed and combo bonuses cut this to 3-6). */
    fun enemyHits(floor: Int): Int = (5 + floor / 15).coerceAtMost(7)

    fun enemyMaxHp(floor: Int, type: EnemyType): Int = (SPELL_DAMAGE * enemyHits(floor) * type.hpFactor).roundToInt()

    fun enemyDamage(floor: Int, type: EnemyType): Int = ((10 + floor) * type.damageFactor).roundToInt().coerceAtMost(35)

    /** 8 s on easy problems up to 20 s on the hardest; relaxed mode gives 75% more. */
    fun timeLimitMillis(difficulty: Int, relaxed: Boolean): Long {
        val base = 8_000L + difficulty * 120L
        return if (relaxed) base * 7 / 4 else base
    }

    /** ×1.5 in the first third of the timer, ×1.25 in the second, ×1 after. */
    fun speedMultiplier(elapsedMillis: Long, limitMillis: Long): Double {
        val fraction = elapsedMillis.toDouble() / limitMillis
        return when {
            fraction <= 1.0 / 3 -> 1.5
            fraction <= 2.0 / 3 -> 1.25
            else -> 1.0
        }
    }

    fun comboMultiplier(streak: Int): Double = when {
        streak >= 5 -> 1.5
        streak >= 3 -> 1.25
        else -> 1.0
    }
}

/**
 * One climb of the spire. Deterministic for a given [RunConfig.seed] and sequence of answers.
 * Not thread-safe; owned by a single caller (the ViewModel).
 */
class RunEngine(
    private val config: RunConfig,
    private val adaptive: AdaptiveDifficulty = AdaptiveDifficulty(),
) {
    private var problemIndex = 0

    var state: RunState = RunState(
        floor = 1,
        playerHp = config.playerMaxHp,
        playerMaxHp = config.playerMaxHp,
        enemy = spawnEnemy(1),
        problem = nextProblem(1),
        timeLimitMillis = 0,
        streak = 0,
        phase = Phase.FIGHTING,
        stats = RunStats(),
    ).let { it.copy(timeLimitMillis = Balance.timeLimitMillis(it.problem.difficulty, config.relaxed)) }
        private set

    fun answer(choice: String, elapsedMillis: Long): AnswerOutcome {
        check(state.phase == Phase.FIGHTING) { "Cannot answer during ${state.phase}" }
        val s = state
        val problem = s.problem
        val correct = problem.isCorrect(choice)
        adaptive.record(config.school, AdaptiveDifficulty.Answer(correct, elapsedMillis, s.timeLimitMillis))
        return if (correct) hit(s, elapsedMillis) else miss(s, timedOut = false)
    }

    fun timeOut(): AnswerOutcome {
        check(state.phase == Phase.FIGHTING) { "Cannot time out during ${state.phase}" }
        val s = state
        adaptive.record(config.school, AdaptiveDifficulty.Answer(false, s.timeLimitMillis, s.timeLimitMillis))
        return miss(s, timedOut = true)
    }

    /** Leaves a cleared floor: heals a little and spawns the next enemy. */
    fun nextFloor() {
        check(state.phase == Phase.FLOOR_CLEARED) { "Floor not cleared" }
        val floor = state.floor + 1
        problemIndex = 0
        state = withProblem(
            state.copy(
                floor = floor,
                playerHp = (state.playerHp + Balance.FLOOR_HEAL).coerceAtMost(state.playerMaxHp),
                enemy = spawnEnemy(floor),
                phase = Phase.FIGHTING,
            ),
        )
    }

    private fun hit(s: RunState, elapsedMillis: Long): AnswerOutcome.Hit {
        val streak = s.streak + 1
        val speed = Balance.speedMultiplier(elapsedMillis, s.timeLimitMillis)
        val combo = Balance.comboMultiplier(streak)
        val damage = (Balance.SPELL_DAMAGE * speed * combo).roundToInt()
        val enemy = s.enemy.copy(hp = (s.enemy.hp - damage).coerceAtLeast(0))
        val fast = speed > 1.0
        val stats = s.stats.copy(
            answered = s.stats.answered + 1,
            correct = s.stats.correct + 1,
            fastAnswers = s.stats.fastAnswers + if (fast) 1 else 0,
            bestStreak = maxOf(s.stats.bestStreak, streak),
            damageDealt = s.stats.damageDealt + damage,
            floorsCleared = s.stats.floorsCleared + if (enemy.defeated) 1 else 0,
        )
        val next = s.copy(enemy = enemy, streak = streak, stats = stats)
        state = if (enemy.defeated) next.copy(phase = Phase.FLOOR_CLEARED) else withProblem(next)
        return AnswerOutcome.Hit(s.problem, damage, fast, combo, enemy.defeated)
    }

    private fun miss(s: RunState, timedOut: Boolean): AnswerOutcome.Miss {
        val damage = Balance.enemyDamage(s.floor, s.enemy.type)
        val hp = (s.playerHp - damage).coerceAtLeast(0)
        val next = s.copy(playerHp = hp, streak = 0, stats = s.stats.copy(answered = s.stats.answered + 1))
        state = if (hp == 0) next.copy(phase = Phase.GAME_OVER) else withProblem(next)
        return AnswerOutcome.Miss(s.problem, damage, timedOut, playerDefeated = hp == 0)
    }

    private fun withProblem(s: RunState): RunState {
        val problem = nextProblem(s.floor)
        return s.copy(problem = problem, timeLimitMillis = Balance.timeLimitMillis(problem.difficulty, config.relaxed))
    }

    private fun nextProblem(floor: Int): Problem {
        val difficulty = adaptive.difficultyFor(config.school, floor)
        return Problems.generate(config.school, difficulty, Seeds.problem(config.seed, floor, problemIndex++))
    }

    private fun spawnEnemy(floor: Int): Enemy {
        val type = Bestiary.forSchool(config.school).random(Random(Seeds.mix(config.seed, floor.toLong(), ENEMY_SALT)))
        val hp = Balance.enemyMaxHp(floor, type)
        return Enemy(type, hp, hp)
    }

    private companion object {
        const val ENEMY_SALT = 0xE7E7L
    }
}
