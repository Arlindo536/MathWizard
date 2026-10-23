package com.arlindodishi.mathwizard.engine

import kotlin.random.Random

/**
 * Light school: perimeter, area, volume, angles and right triangles as short word problems.
 * Each problem carries a [Problem.promptKey] and [Problem.promptArgs] that the app turns into
 * a localised sentence; [Problem.prompt] holds the English text.
 *
 * Tiers: squares and rectangles (0), triangles, angles, boxes (25), Pythagorean hypotenuse,
 * quadrilateral angles, trapezoids (50), missing legs and polygon angles (75).
 */
object GeometryGenerator : ProblemGenerator {
    override val school = School.GEOMETRY

    /** English templates; %1..%3 are replaced by the arguments. */
    val templates: Map<String, String> = mapOf(
        "square_perimeter" to "Perimeter of a square with side %1",
        "rect_perimeter" to "Perimeter of a %1 × %2 rectangle",
        "rect_area" to "Area of a %1 × %2 rectangle",
        "triangle_area" to "Area of a triangle with base %1 and height %2",
        "triangle_angle" to "A triangle has angles %1° and %2°. The third angle?",
        "box_volume" to "Volume of a %1 × %2 × %3 box",
        "hypotenuse" to "A right triangle has legs %1 and %2. The hypotenuse?",
        "quad_angle" to "A quadrilateral has angles %1°, %2° and %3°. The fourth angle?",
        "trapezoid_area" to "Area of a trapezoid with parallel sides %1 and %2 and height %3",
        "missing_leg" to "A right triangle has a leg %1 and hypotenuse %2. The other leg?",
        "polygon_angle_sum" to "Sum of the interior angles of a polygon with %1 sides",
        "regular_polygon_angle" to "Each interior angle of a regular polygon with %1 sides",
        "cutout_area" to "A %1 × %2 rectangle with a %3 × %3 square cut out. Area left?",
    )

    private data class Draft(val key: String, val args: List<Int>, val answer: Int, val candidates: List<Int>)

    private enum class Kind(val unlockAt: Int) {
        SQUARE_PERIMETER(0), RECT_PERIMETER(0), RECT_AREA(0),
        TRIANGLE_AREA(25), TRIANGLE_ANGLE(25), BOX_VOLUME(25),
        HYPOTENUSE(50), QUAD_ANGLE(50), TRAPEZOID_AREA(50),
        MISSING_LEG(75), POLYGON_ANGLE_SUM(75), REGULAR_POLYGON_ANGLE(75),
    }

    private val triples = listOf(Triple(3, 4, 5), Triple(5, 12, 13), Triple(8, 15, 17), Triple(7, 24, 25))
    private val regularPolygonSides = listOf(3, 4, 5, 6, 8, 9, 10, 12)

    override fun generate(difficulty: Int, random: Random): Problem {
        val d = difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        return problem(d, draft(Kind.entries.filter { d >= it.unlockAt }.random(random), d, random), random)
    }

    /** Boss: a cut-out area or a top-tier problem with bigger numbers. */
    override fun generateBoss(difficulty: Int, random: Random): Problem {
        val d = difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        val draft = if (d < 50 || random.nextBoolean()) cutout(d, random) else {
            draft(listOf(Kind.MISSING_LEG, Kind.TRAPEZOID_AREA, Kind.REGULAR_POLYGON_ANGLE).random(random), 100, random)
        }
        return problem(d, draft, random)
    }

    fun render(key: String, args: List<String>): String =
        args.foldIndexed(templates.getValue(key)) { i, text, arg -> text.replace("%${i + 1}", arg) }

    private fun problem(d: Int, draft: Draft, random: Random): Problem {
        val args = draft.args.map(Int::toString)
        return Problem(
            school = school,
            difficulty = d,
            prompt = render(draft.key, args),
            answer = draft.answer.toString(),
            choices = buildIntChoices(draft.answer, draft.candidates + listOf(draft.answer + 2, draft.answer - 2), random) { it > 0 },
            promptKey = draft.key,
            promptArgs = args,
        )
    }

    private fun side(d: Int, random: Random) = random.nextInt(2, 6 + d / 8)

    private fun draft(kind: Kind, d: Int, random: Random): Draft = when (kind) {
        Kind.SQUARE_PERIMETER -> {
            val s = side(d, random)
            Draft("square_perimeter", listOf(s), 4 * s, listOf(s * s, 2 * s, 4 * s + 4))
        }
        Kind.RECT_PERIMETER -> {
            val w = side(d, random)
            val h = side(d, random)
            Draft("rect_perimeter", listOf(w, h), 2 * (w + h), listOf(w * h, w + h, 2 * w + h))
        }
        Kind.RECT_AREA -> {
            val w = side(d, random)
            val h = side(d, random)
            Draft("rect_area", listOf(w, h), w * h, listOf(2 * (w + h), w + h, w * h + w))
        }
        Kind.TRIANGLE_AREA -> {
            val base = 2 * random.nextInt(1, 4 + d / 10)
            val height = side(d, random)
            Draft("triangle_area", listOf(base, height), base * height / 2, listOf(base * height, base + height, base * height / 2 + height))
        }
        Kind.TRIANGLE_ANGLE -> {
            val a = 5 * random.nextInt(4, 16)
            val b = 5 * random.nextInt(4, (35 - a / 5).coerceAtLeast(5))
            if (a + b >= 175) {
                draft(Kind.TRIANGLE_AREA, d, random)
            } else {
                Draft("triangle_angle", listOf(a, b), 180 - a - b, listOf(360 - a - b, a + b, 90 - (a + b) / 2))
            }
        }
        Kind.BOX_VOLUME -> {
            val a = random.nextInt(2, 7)
            val b = random.nextInt(2, 7)
            val c = random.nextInt(2, 7)
            Draft("box_volume", listOf(a, b, c), a * b * c, listOf(a + b + c, 2 * (a * b + b * c + a * c), a * b + c))
        }
        Kind.HYPOTENUSE -> {
            val (x, y, z) = triples.random(random)
            val k = if (x == 3) random.nextInt(1, 5) else 1
            Draft("hypotenuse", listOf(k * x, k * y), k * z, listOf(k * x + k * y, k * z + 1, k * (z - 1)))
        }
        Kind.QUAD_ANGLE -> {
            val a = 10 * random.nextInt(6, 13)
            val b = 10 * random.nextInt(6, 13)
            val c = 10 * random.nextInt(6, 13)
            Draft("quad_angle", listOf(a, b, c), 360 - a - b - c, listOf(540 - a - b - c, 180 - (a + b + c) % 180, 360 - a - b))
        }
        Kind.TRAPEZOID_AREA -> {
            val a = random.nextInt(2, 9)
            val b = a + 2 * random.nextInt(1, 4)
            val h = random.nextInt(2, 9)
            Draft("trapezoid_area", listOf(a, b, h), (a + b) * h / 2, listOf((a + b) * h, a * b * h / 2, a * h + b))
        }
        Kind.MISSING_LEG -> {
            val (x, y, z) = triples.random(random)
            val k = if (x == 3) random.nextInt(1, 4) else 1
            if (random.nextBoolean()) {
                Draft("missing_leg", listOf(k * x, k * z), k * y, listOf(k * z - k * x, k * z + k * x, k * y + 1))
            } else {
                Draft("missing_leg", listOf(k * y, k * z), k * x, listOf(k * z - k * y, k * z + k * y, k * x + 1))
            }
        }
        Kind.POLYGON_ANGLE_SUM -> {
            val n = random.nextInt(5, 11)
            Draft("polygon_angle_sum", listOf(n), (n - 2) * 180, listOf(n * 180, (n - 1) * 180, 360))
        }
        Kind.REGULAR_POLYGON_ANGLE -> {
            val n = regularPolygonSides.random(random)
            val answer = (n - 2) * 180 / n
            Draft("regular_polygon_angle", listOf(n), answer, listOf(360 / n, 180 - answer + 10, (n - 2) * 180))
        }
    }

    private fun cutout(d: Int, random: Random): Draft {
        val w = random.nextInt(5, 9 + d / 10)
        val h = random.nextInt(5, 9 + d / 10)
        val s = random.nextInt(2, minOf(w, h))
        return Draft("cutout_area", listOf(w, h, s), w * h - s * s, listOf(w * h, w * h - 4 * s, (w - s) * (h - s)))
    }
}
