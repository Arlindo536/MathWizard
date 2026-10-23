package com.arlindodishi.mathwizard.ui.schools

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.engine.Problem
import com.arlindodishi.mathwizard.engine.School

/** Localised texts for a school; its icon is [com.arlindodishi.mathwizard.ui.pixel.Sprites.school]. */
data class SchoolUi(@param:StringRes val name: Int, @param:StringRes val description: Int)

fun schoolUi(school: School): SchoolUi = when (school) {
    School.ARITHMETIC -> SchoolUi(R.string.school_arithmetic, R.string.school_arithmetic_desc)
    School.FRACTIONS -> SchoolUi(R.string.school_fractions, R.string.school_fractions_desc)
    School.ALGEBRA -> SchoolUi(R.string.school_algebra, R.string.school_algebra_desc)
    School.POWERS -> SchoolUi(R.string.school_powers, R.string.school_powers_desc)
    School.GEOMETRY -> SchoolUi(R.string.school_geometry, R.string.school_geometry_desc)
    School.SEQUENCES -> SchoolUi(R.string.school_sequences, R.string.school_sequences_desc)
}

/**
 * The problem text to show: localised for word problems, the symbolic prompt otherwise.
 * Superscript digits (7²) become raised small normal digits, because the pixel font has no
 * superscript glyphs.
 */
@Composable
fun problemText(problem: Problem): AnnotatedString {
    val key = problem.promptKey
    val res = key?.let { geometryPrompts[it] }
    val text = if (res != null) stringResource(res, *problem.promptArgs.toTypedArray()) else problem.prompt
    return withRaisedExponents(text)
}

private const val SUPERSCRIPT_DIGITS = "\u2070\u00B9\u00B2\u00B3\u2074\u2075\u2076\u2077\u2078\u2079"

fun withRaisedExponents(text: String): AnnotatedString = buildAnnotatedString {
    for (c in text) {
        val digit = SUPERSCRIPT_DIGITS.indexOf(c)
        if (digit < 0) {
            append(c)
        } else {
            withStyle(SpanStyle(baselineShift = BaselineShift(0.55f), fontSize = 0.6.em)) { append('0' + digit) }
        }
    }
}

private val geometryPrompts: Map<String, Int> = mapOf(
    "square_perimeter" to R.string.geo_square_perimeter,
    "rect_perimeter" to R.string.geo_rect_perimeter,
    "rect_area" to R.string.geo_rect_area,
    "triangle_area" to R.string.geo_triangle_area,
    "triangle_angle" to R.string.geo_triangle_angle,
    "box_volume" to R.string.geo_box_volume,
    "hypotenuse" to R.string.geo_hypotenuse,
    "quad_angle" to R.string.geo_quad_angle,
    "trapezoid_area" to R.string.geo_trapezoid_area,
    "missing_leg" to R.string.geo_missing_leg,
    "polygon_angle_sum" to R.string.geo_polygon_angle_sum,
    "regular_polygon_angle" to R.string.geo_regular_polygon_angle,
    "cutout_area" to R.string.geo_cutout_area,
)
