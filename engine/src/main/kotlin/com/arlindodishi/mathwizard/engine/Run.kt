package com.arlindodishi.mathwizard.engine

import kotlin.math.roundToInt
import kotlin.random.Random

data class RunConfig(
    val seed: Long,
    val school: School = School.ARITHMETIC,
    /** Longer timers for players who want less pressure. */
    val relaxed: Boolean = false,
    val playerMaxHp: Int = Balance.PLAYER_MAX_HP,
    /** Permanent upgrades bought between runs. */
    val bonuses: RunBonuses = RunBonuses(),
    /**
     * Adapt difficulty to the player's speed and accuracy. Off for the Daily Rift, so every
     * player meets the same problems for the same answers.
     */
    val adaptive: Boolean = true,
)

enum class Phase { FIGHTING, FLOOR_CLEARED, GAME_OVER }

data class RunStats(
    val answered: Int = 0,
    val correct: Int = 0,
    val fastAnswers: Int = 0,
    val bestStreak: Int = 0,
    val floorsCleared: Int = 0,
    val bossesDefeated: Int = 0,
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
    val relics: List<Relic> = emptyList(),
    /** Relics to choose from after clearing a floor; empty when all are held. */
    val relicOffer: List<Relic> = emptyList(),
    /** Mana Shield is unused on this floor. */
    val shieldReady: Boolean = false,
    /** Phoenix Feather has not been consumed yet. */
    val reviveReady: Boolean = false,
    val hintsLeft: Int = 0,
) {
    val isBossFloor: Boolean get() = enemy.type.boss
}

sealed interface AnswerOutcome {
    /** The answered problem, for showing feedback while [RunState.problem] has moved on. */
    val problem: Problem

    data class Hit(
        override val problem: Problem,
        val damage: Int,
        val fast: Boolean,
        val comboMultiplier: Double,
        val enemyDefeated: Boolean,
        val healed: Int = 0,
    ) : AnswerOutcome

    data class Miss(
        override val problem: Problem,
        val damageTaken: Int,
        val timedOut: Boolean,
        val playerDefeated: Boolean,
        /** Mana Shield absorbed this miss. */
        val shielded: Boolean = false,
        /** Phoenix Feather saved the player from this miss. */
        val revived: Boolean = false,
    ) : AnswerOutcome
}

/** Tunable numbers for the run loop, kept together for balancing passes. */
object Balance {
    const val PLAYER_MAX_HP = 100
    const val SPELL_DAMAGE = 25
    const val FLOOR_HEAL = 15
    const val BOSS_EVERY = 5
    const val BOSS_HP_MULTIPLIER = 1.5
    const val BOSS_DAMAGE_MULTIPLIER = 1.25
    const val BOSS_TIME_MULTIPLIER = 1.5

    fun isBossFloor(floor: Int): Boolean = floor % BOSS_EVERY == 0

    /** Base hits to defeat an enemy: 5 early, rising to 7 (speed and combo bonuses cut this to 3-6). */
    fun enemyHits(floor: Int): Int = (5 + floor / 15).coerceAtMost(7)

    fun enemyMaxHp(floor: Int, type: EnemyType): Int {
        val hp = SPELL_DAMAGE * enemyHits(floor) * type.hpFactor
        return (if (type.boss) hp * BOSS_HP_MULTIPLIER else hp).roundToInt()
    }

    fun enemyDamage(floor: Int, type: EnemyType): Int {
        val damage = ((10 + floor) * type.damageFactor).coerceAtMost(35.0)
        return (if (type.boss) damage * BOSS_DAMAGE_MULTIPLIER else damage).roundToInt()
    }

    /** 8 s on easy problems up to 20 s on the hardest; relaxed mode gives 75% more. */
    fun timeLimitMillis(difficulty: Int, relaxed: Boolean): Long {
        val base = 8_000L + difficulty * 120L
        return if (relaxed) base * 7 / 4 else base
    }

    /** ×1.5 in the first third of the timer, ×1.25 in the second, ×1 after (halves and quarters if [wider]). */
    fun speedMultiplier(elapsedMillis: Long, limitMillis: Long, wider: Boolean = false): Double {
        val fraction = elapsedMillis.toDouble() / limitMillis
        val (fast, medium) = if (wider) 0.5 to 0.75 else 1.0 / 3 to 2.0 / 3
        return when {
            fraction <= fast -> 1.5
            fraction <= medium -> 1.25
            else -> 1.0
        }
    }

    /** ×1.25 from 3 in a row, ×1.5 from 5; [shift] lowers both thresholds. */
    fun comboMultiplier(streak: Int, shift: Int = 0): Double = when {
        streak >= 5 - shift -> 1.5
        streak >= 3 - shift -> 1.25
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
    private var modifiers = Modifiers()

    var state: RunState = withProblem(
        RunState(
            floor = 1,
            playerHp = config.playerMaxHp + config.bonuses.maxHpBonus,
            playerMaxHp = config.playerMaxHp + config.bonuses.maxHpBonus,
            enemy = spawnEnemy(1),
            problem = PLACEHOLDER_PROBLEM,
            timeLimitMillis = 0,
            streak = 0,
            phase = Phase.FIGHTING,
            stats = RunStats(),
            hintsLeft = config.bonuses.hints,
        ).let { if (config.bonuses.startingRelic) withRelic(it, startingRelic()) else it },
    )
        private set

    val currentModifiers: Modifiers get() = modifiers

    fun answer(choice: String, elapsedMillis: Long): AnswerOutcome {
        check(state.phase == Phase.FIGHTING) { "Cannot answer during ${state.phase}" }
        val s = state
        val correct = s.problem.isCorrect(choice)
        adaptive.record(config.school, AdaptiveDifficulty.Answer(correct, elapsedMillis, s.timeLimitMillis))
        return if (correct) hit(s, elapsedMillis) else miss(s, timedOut = false)
    }

    fun timeOut(): AnswerOutcome {
        check(state.phase == Phase.FIGHTING) { "Cannot time out during ${state.phase}" }
        val s = state
        adaptive.record(config.school, AdaptiveDifficulty.Answer(false, s.timeLimitMillis, s.timeLimitMillis))
        return miss(s, timedOut = true)
    }

    /**
     * Leaves a cleared floor, taking [relicId] from the current offer (or none, to skip),
     * then heals and spawns the next enemy.
     */
    fun nextFloor(relicId: String? = null) {
        check(state.phase == Phase.FLOOR_CLEARED) { "Floor not cleared" }
        var s = state
        if (relicId != null) {
            val relic = requireNotNull(s.relicOffer.find { it.id == relicId }) { "$relicId is not on offer" }
            s = withRelic(s, relic)
        }
        val floor = s.floor + 1
        val heal = Balance.FLOOR_HEAL + modifiers.floorHealBonus + config.bonuses.floorHealBonus
        problemIndex = 0
        state = withProblem(
            s.copy(
                floor = floor,
                playerHp = (s.playerHp + heal).coerceAtMost(s.playerMaxHp),
                enemy = spawnEnemy(floor),
                phase = Phase.FIGHTING,
                relicOffer = emptyList(),
                shieldReady = modifiers.missShield,
            ),
        )
    }

    /**
     * Hides wrong choices until two remain (the answer and one wrong). Returns false when no
     * hint is left, the problem already has two choices, or the player is not fighting.
     */
    fun useHint(): Boolean {
        val s = state
        if (s.phase != Phase.FIGHTING || s.hintsLeft <= 0 || s.problem.choices.size <= 2) return false
        val keptWrong = s.problem.choices.first { it != s.problem.answer }
        val choices = s.problem.choices.filter { it == s.problem.answer || it == keptWrong }
        state = s.copy(problem = s.problem.copy(choices = choices), hintsLeft = s.hintsLeft - 1)
        return true
    }

    private fun withRelic(s: RunState, relic: Relic): RunState {
        modifiers = Modifiers.of(s.relics + relic)
        return s.copy(
            relics = s.relics + relic,
            playerMaxHp = s.playerMaxHp + relic.maxHpBonus,
            playerHp = s.playerHp + relic.maxHpBonus,
            reviveReady = s.reviveReady || relic.reviveHp > 0,
            shieldReady = s.shieldReady || relic.missShield,
        )
    }

    private fun startingRelic(): Relic = Relics.all.random(Random(Seeds.mix(config.seed, START_RELIC_SALT)))

    private fun hit(s: RunState, elapsedMillis: Long): AnswerOutcome.Hit {
        val streak = s.streak + 1
        val speed = Balance.speedMultiplier(elapsedMillis, s.timeLimitMillis, modifiers.widerSpeedWindow)
        val combo = Balance.comboMultiplier(streak, modifiers.comboThresholdShift)
        val bossBonus = if (s.isBossFloor) modifiers.bossDamageMultiplier else 1.0
        val multiplier = modifiers.damageMultiplier * config.bonuses.damageMultiplier * bossBonus
        val damage = (Balance.SPELL_DAMAGE * speed * combo * multiplier).roundToInt()
        val enemy = s.enemy.copy(hp = (s.enemy.hp - damage).coerceAtLeast(0))
        val healed = minOf(modifiers.healPerHit, s.playerMaxHp - s.playerHp)
        val fast = speed > 1.0
        val stats = s.stats.copy(
            answered = s.stats.answered + 1,
            correct = s.stats.correct + 1,
            fastAnswers = s.stats.fastAnswers + if (fast) 1 else 0,
            bestStreak = maxOf(s.stats.bestStreak, streak),
            damageDealt = s.stats.damageDealt + damage,
            floorsCleared = s.stats.floorsCleared + if (enemy.defeated) 1 else 0,
            bossesDefeated = s.stats.bossesDefeated + if (enemy.defeated && s.isBossFloor) 1 else 0,
        )
        val next = s.copy(enemy = enemy, streak = streak, stats = stats, playerHp = s.playerHp + healed)
        state = if (enemy.defeated) {
            val offer = Relics.offer(s.relics, relicRandom(s.floor), config.bonuses.relicOfferSize)
            next.copy(phase = Phase.FLOOR_CLEARED, relicOffer = offer)
        } else {
            withProblem(next)
        }
        return AnswerOutcome.Hit(s.problem, damage, fast, combo, enemy.defeated, healed)
    }

    private fun miss(s: RunState, timedOut: Boolean): AnswerOutcome.Miss {
        val stats = s.stats.copy(answered = s.stats.answered + 1)
        if (s.shieldReady) {
            state = withProblem(s.copy(streak = 0, stats = stats, shieldReady = false))
            return AnswerOutcome.Miss(s.problem, 0, timedOut, playerDefeated = false, shielded = true)
        }
        val damage = (Balance.enemyDamage(s.floor, s.enemy.type) * modifiers.enemyDamageMultiplier).roundToInt()
        var hp = (s.playerHp - damage).coerceAtLeast(0)
        val revived = hp == 0 && s.reviveReady
        if (revived) hp = minOf(modifiers.reviveHp, s.playerMaxHp)
        val next = s.copy(playerHp = hp, streak = 0, stats = stats, reviveReady = s.reviveReady && !revived)
        state = if (hp == 0) next.copy(phase = Phase.GAME_OVER) else withProblem(next)
        return AnswerOutcome.Miss(s.problem, damage, timedOut, playerDefeated = hp == 0, revived = revived)
    }

    private fun withProblem(s: RunState): RunState {
        val boss = s.isBossFloor
        val difficulty = if (config.adaptive) adaptive.difficultyFor(config.school, s.floor) else DifficultyCurve.base(s.floor)
        var problem = Problems.generate(config.school, difficulty, Seeds.problem(config.seed, s.floor, problemIndex++), boss)
        if (modifiers.removesWrongChoice) {
            problem = problem.copy(choices = problem.choices - problem.choices.first { it != problem.answer })
        }
        var limit = Balance.timeLimitMillis(problem.difficulty, config.relaxed) *
            modifiers.timeMultiplier * config.bonuses.timeMultiplier
        if (boss) limit *= Balance.BOSS_TIME_MULTIPLIER
        return s.copy(problem = problem, timeLimitMillis = limit.toLong())
    }

    private fun spawnEnemy(floor: Int): Enemy {
        val type = if (Balance.isBossFloor(floor)) {
            Bestiary.bossFor(config.school)
        } else {
            Bestiary.forSchool(config.school).random(Random(Seeds.mix(config.seed, floor.toLong(), ENEMY_SALT)))
        }
        val hp = Balance.enemyMaxHp(floor, type)
        return Enemy(type, hp, hp)
    }

    private fun relicRandom(floor: Int) = Random(Seeds.mix(config.seed, floor.toLong(), RELIC_SALT))

    private companion object {
        const val ENEMY_SALT = 0xE7E7L
        const val RELIC_SALT = 0x8E11CL
        const val START_RELIC_SALT = 0x5747L
        val PLACEHOLDER_PROBLEM = Problem(School.ARITHMETIC, 0, "", "", emptyList())
    }
}
