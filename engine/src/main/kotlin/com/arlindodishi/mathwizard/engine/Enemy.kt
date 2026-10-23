package com.arlindodishi.mathwizard.engine

/** Static description of an enemy; the app maps [id] to art and a localised name. */
data class EnemyType(
    val id: String,
    val school: School,
    val hpFactor: Double,
    val damageFactor: Double,
    val boss: Boolean = false,
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
        EnemyType("inferno_drake", School.ARITHMETIC, hpFactor = 1.0, damageFactor = 1.0, boss = true),

        EnemyType("tide_sprite", School.FRACTIONS, hpFactor = 0.9, damageFactor = 1.0),
        EnemyType("frost_eel", School.FRACTIONS, hpFactor = 1.0, damageFactor = 1.1),
        EnemyType("mist_crab", School.FRACTIONS, hpFactor = 1.2, damageFactor = 0.8),
        EnemyType("abyssal_leviathan", School.FRACTIONS, hpFactor = 1.0, damageFactor = 1.0, boss = true),

        EnemyType("spark_bat", School.ALGEBRA, hpFactor = 0.9, damageFactor = 1.2),
        EnemyType("storm_beetle", School.ALGEBRA, hpFactor = 1.2, damageFactor = 0.9),
        EnemyType("volt_serpent", School.ALGEBRA, hpFactor = 1.0, damageFactor = 1.0),
        EnemyType("thunder_titan", School.ALGEBRA, hpFactor = 1.0, damageFactor = 1.0, boss = true),

        EnemyType("burrow_mole", School.POWERS, hpFactor = 0.9, damageFactor = 1.0),
        EnemyType("stone_tortoise", School.POWERS, hpFactor = 1.3, damageFactor = 0.8),
        EnemyType("thorn_beast", School.POWERS, hpFactor = 1.0, damageFactor = 1.2),
        EnemyType("mountain_giant", School.POWERS, hpFactor = 1.0, damageFactor = 1.0, boss = true),

        EnemyType("prism_moth", School.GEOMETRY, hpFactor = 0.9, damageFactor = 1.1),
        EnemyType("halo_wisp", School.GEOMETRY, hpFactor = 0.95, damageFactor = 1.0),
        EnemyType("mirror_knight", School.GEOMETRY, hpFactor = 1.2, damageFactor = 1.0),
        EnemyType("solar_sphinx", School.GEOMETRY, hpFactor = 1.0, damageFactor = 1.0, boss = true),

        EnemyType("shade_cat", School.SEQUENCES, hpFactor = 0.9, damageFactor = 1.2),
        EnemyType("echo_wraith", School.SEQUENCES, hpFactor = 1.0, damageFactor = 1.0),
        EnemyType("void_spider", School.SEQUENCES, hpFactor = 1.1, damageFactor = 0.9),
        EnemyType("night_lich", School.SEQUENCES, hpFactor = 1.0, damageFactor = 1.0, boss = true),
    )

    /** Regular (non-boss) enemies of a school. */
    fun forSchool(school: School): List<EnemyType> = all.filter { it.school == school && !it.boss }

    fun bossFor(school: School): EnemyType = all.first { it.school == school && it.boss }
}
