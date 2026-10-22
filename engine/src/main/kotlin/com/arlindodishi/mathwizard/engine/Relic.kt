package com.arlindodishi.mathwizard.engine

import kotlin.random.Random

/**
 * A run-only upgrade. Effects are plain data so new relics are added to [Relics.all]
 * without touching the run logic; [Modifiers] folds the held relics into one set of numbers.
 */
data class Relic(
    val id: String,
    val damageMultiplier: Double = 1.0,
    val bossDamageMultiplier: Double = 1.0,
    val timeMultiplier: Double = 1.0,
    val maxHpBonus: Int = 0,
    val floorHealBonus: Int = 0,
    val enemyDamageMultiplier: Double = 1.0,
    /** Speed bonus windows become the first half / three quarters of the timer. */
    val widerSpeedWindow: Boolean = false,
    /** Combo tiers start this many answers earlier. */
    val comboThresholdShift: Int = 0,
    /** The first miss on each floor deals no damage. */
    val missShield: Boolean = false,
    val healPerHit: Int = 0,
    /** One wrong choice is removed from every problem. */
    val removesWrongChoice: Boolean = false,
    /** Once per run, a lethal hit leaves the player at this much HP instead. */
    val reviveHp: Int = 0,
)

object Relics {
    val all: List<Relic> = listOf(
        Relic("ember_focus", damageMultiplier = 1.2),
        Relic("quicksilver_quill", timeMultiplier = 1.25),
        Relic("heartstone", maxHpBonus = 25),
        Relic("healing_draught", floorHealBonus = 15),
        Relic("iron_ward", enemyDamageMultiplier = 0.75),
        Relic("swift_sigil", widerSpeedWindow = true),
        Relic("combo_crown", comboThresholdShift = 1),
        Relic("mana_shield", missShield = true),
        Relic("vampiric_rune", healPerHit = 3),
        Relic("oracle_eye", removesWrongChoice = true),
        Relic("phoenix_feather", reviveHp = 30),
        Relic("giant_slayer", bossDamageMultiplier = 1.5),
    )

    const val OFFER_SIZE = 3

    fun byId(id: String): Relic = requireNotNull(all.find { it.id == id }) { "Unknown relic $id" }

    /** Up to [OFFER_SIZE] distinct relics the player does not hold yet. */
    fun offer(held: List<Relic>, random: Random): List<Relic> =
        all.filter { it !in held }.shuffled(random).take(OFFER_SIZE)
}

/** Combined effect of all held relics. */
data class Modifiers(
    val damageMultiplier: Double = 1.0,
    val bossDamageMultiplier: Double = 1.0,
    val timeMultiplier: Double = 1.0,
    val maxHpBonus: Int = 0,
    val floorHealBonus: Int = 0,
    val enemyDamageMultiplier: Double = 1.0,
    val widerSpeedWindow: Boolean = false,
    val comboThresholdShift: Int = 0,
    val missShield: Boolean = false,
    val healPerHit: Int = 0,
    val removesWrongChoice: Boolean = false,
    val reviveHp: Int = 0,
) {
    companion object {
        fun of(relics: List<Relic>): Modifiers = relics.fold(Modifiers()) { m, r ->
            Modifiers(
                damageMultiplier = m.damageMultiplier * r.damageMultiplier,
                bossDamageMultiplier = m.bossDamageMultiplier * r.bossDamageMultiplier,
                timeMultiplier = m.timeMultiplier * r.timeMultiplier,
                maxHpBonus = m.maxHpBonus + r.maxHpBonus,
                floorHealBonus = m.floorHealBonus + r.floorHealBonus,
                enemyDamageMultiplier = m.enemyDamageMultiplier * r.enemyDamageMultiplier,
                widerSpeedWindow = m.widerSpeedWindow || r.widerSpeedWindow,
                comboThresholdShift = m.comboThresholdShift + r.comboThresholdShift,
                missShield = m.missShield || r.missShield,
                healPerHit = m.healPerHit + r.healPerHit,
                removesWrongChoice = m.removesWrongChoice || r.removesWrongChoice,
                reviveHp = maxOf(m.reviveHp, r.reviveHp),
            )
        }
    }
}
