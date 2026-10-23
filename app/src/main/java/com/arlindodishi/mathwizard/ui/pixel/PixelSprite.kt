package com.arlindodishi.mathwizard.ui.pixel

/**
 * A pixel-art image: [rows] of palette characters ('.' is transparent) and the ARGB colour
 * of each character. Plain Kotlin so sprite data can be checked in JVM unit tests.
 */
class PixelSprite(val rows: List<String>, val palette: Map<Char, Long>) {
    val width: Int = rows.firstOrNull()?.length ?: 0
    val height: Int = rows.size

    /** Horizontal runs of one colour, so drawing needs one rect per run instead of per pixel. */
    val runs: List<Run> = buildList {
        rows.forEachIndexed { y, row ->
            var x = 0
            while (x < row.length) {
                val c = row[x]
                var end = x + 1
                while (end < row.length && row[end] == c) end++
                if (c != TRANSPARENT) add(Run(x, y, end - x, palette.getValue(c)))
                x = end
            }
        }
    }

    data class Run(val x: Int, val y: Int, val length: Int, val argb: Long)

    companion object {
        const val TRANSPARENT = '.'
    }
}

/**
 * Colour slots shared by all templates: K outline, 1 base, 2 light, 3 shadow, E eye glow,
 * P pupil, A accent (horns, teeth, details), W white.
 */
data class Palette(
    val outline: Long,
    val base: Long,
    val light: Long,
    val shadow: Long,
    val eye: Long,
    val accent: Long,
    val pupil: Long = 0xFF1A0D10,
    val white: Long = 0xFFFFF6E0,
) {
    fun toMap(): Map<Char, Long> = mapOf(
        'K' to outline, '1' to base, '2' to light, '3' to shadow,
        'E' to eye, 'P' to pupil, 'A' to accent, 'W' to white,
    )
}

/** Parses a template block: one row per line, surrounding indentation removed. */
fun template(text: String): List<String> = text.trimIndent().lines().filter { it.isNotBlank() }

fun sprite(rows: List<String>, palette: Palette) = PixelSprite(rows, palette.toMap())
