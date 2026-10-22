package com.arlindodishi.mathwizard.engine

/** Static description of an enemy; the app maps [id] to art and a localised name. */
data class EnemyType(
    val id: String,
    val school: School,
    val hpFactor: Double,
    val damageFactor: Double,
)

data class Enemy(val type: EnemyType, val maxHp: Int, val hp: Int) {
    val defeated: Boolean get() = hp <= 0
}

object Bestiary {
    val all: List<EnemyType> = listOf(
        EnemyType("ember_imp", School.ARITHMETIC, hpFactor = 0.9, damageFactor = 0.9),
        EnemyType("cinder_slime", School.ARITHMETIC, hpFactor = 1.2, damageFactor = 0.7),
        EnemyType("ash_goblin", School.ARITHMETIC, hpFactor = 1.0, damageFactor = 1.0),
        EnemyType("flame_wisp", School.ARITHMETIC, hpFactor = 0.9, damageFactor = 1.3),
        EnemyType("magma_golem", School.ARITHMETIC, hpFactor = 1.3, damageFactor = 1.1),
        EnemyType("blaze_hound", School.ARITHMETIC, hpFactor = 0.95, damageFactor = 1.2),
    )

    fun forSchool(school: School): List<EnemyType> = all.filter { it.school == school }
}
