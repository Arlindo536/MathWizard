package com.arlindodishi.mathwizard.engine

enum class Branch { VITALITY, FOCUS, ARCANA }

/**
 * A permanent upgrade bought with Mana crystals between runs. [costs] has one entry per level;
 * [requires] names an upgrade that must have at least [requiresLevel] levels first.
 */
data class Upgrade(
    val id: String,
    val branch: Branch,
    val costs: List<Int>,
    val requires: String? = null,
    val requiresLevel: Int = 1,
) {
    val maxLevel: Int get() = costs.size
}

object Upgrades {
    val all: List<Upgrade> = listOf(
        Upgrade("vitality", Branch.VITALITY, costs = listOf(20, 35, 55, 80, 110)),
        Upgrade("second_wind", Branch.VITALITY, costs = listOf(30, 50, 80), requires = "vitality"),
        Upgrade("focus", Branch.FOCUS, costs = listOf(20, 35, 55, 80)),
        Upgrade("hint", Branch.FOCUS, costs = listOf(40, 70, 110), requires = "focus"),
        Upgrade("power", Branch.ARCANA, costs = listOf(25, 40, 60, 85, 115)),
        Upgrade("relic_insight", Branch.ARCANA, costs = listOf(120), requires = "power", requiresLevel = 2),
        Upgrade("starting_relic", Branch.ARCANA, costs = listOf(150), requires = "relic_insight"),
    )

    fun byId(id: String): Upgrade = requireNotNull(all.find { it.id == id }) { "Unknown upgrade $id" }

    const val VITALITY_HP = 10
    const val SECOND_WIND_HEAL = 5
    const val FOCUS_TIME = 0.08
    const val POWER_DAMAGE = 0.05
}

/** Run-start bonuses derived from purchased upgrades. */
data class RunBonuses(
    val maxHpBonus: Int = 0,
    val floorHealBonus: Int = 0,
    val timeMultiplier: Double = 1.0,
    val damageMultiplier: Double = 1.0,
    val hints: Int = 0,
    val relicOfferSize: Int = Relics.OFFER_SIZE,
    val startingRelic: Boolean = false,
)

/** Everything kept between runs: crystal balance and upgrade levels (0 when absent). */
data class MetaProfile(val crystals: Int = 0, val levels: Map<String, Int> = emptyMap()) {

    fun level(id: String): Int = levels[id] ?: 0

    fun isMaxed(id: String): Boolean = level(id) >= Upgrades.byId(id).maxLevel

    fun prerequisiteMet(id: String): Boolean {
        val upgrade = Upgrades.byId(id)
        return upgrade.requires == null || level(upgrade.requires) >= upgrade.requiresLevel
    }

    /** Cost of the next level, or null when maxed. */
    fun nextCost(id: String): Int? = Upgrades.byId(id).costs.getOrNull(level(id))

    fun canBuy(id: String): Boolean {
        val cost = nextCost(id) ?: return false
        return prerequisiteMet(id) && crystals >= cost
    }

    /** The profile after buying one level of [id], or null if not allowed. */
    fun buy(id: String): MetaProfile? {
        if (!canBuy(id)) return null
        return copy(crystals = crystals - nextCost(id)!!, levels = levels + (id to level(id) + 1))
    }

    fun bonuses(): RunBonuses = RunBonuses(
        maxHpBonus = level("vitality") * Upgrades.VITALITY_HP,
        floorHealBonus = level("second_wind") * Upgrades.SECOND_WIND_HEAL,
        timeMultiplier = 1.0 + level("focus") * Upgrades.FOCUS_TIME,
        damageMultiplier = 1.0 + level("power") * Upgrades.POWER_DAMAGE,
        hints = level("hint"),
        relicOfferSize = Relics.OFFER_SIZE + level("relic_insight"),
        startingRelic = level("starting_relic") > 0,
    )
}

object MetaRewards {
    /** 3 per floor cleared, 10 per boss, 1 per 5 correct answers. */
    fun crystalsFor(stats: RunStats): Int = stats.floorsCleared * 3 + stats.bossesDefeated * 10 + stats.correct / 5
}
