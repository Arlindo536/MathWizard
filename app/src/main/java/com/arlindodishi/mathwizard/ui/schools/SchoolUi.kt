package com.arlindodishi.mathwizard.ui.schools

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.engine.Problem
import com.arlindodishi.mathwizard.engine.School

data class SchoolUi(val emoji: String, @param:StringRes val name: Int, @param:StringRes val description: Int)

fun schoolUi(school: School): SchoolUi = when (school) {
    School.ARITHMETIC -> SchoolUi("🔥", R.string.school_arithmetic, R.string.school_arithmetic_desc)
    School.FRACTIONS -> SchoolUi("💧", R.string.school_fractions, R.string.school_fractions_desc)
    School.ALGEBRA -> SchoolUi("⚡", R.string.school_algebra, R.string.school_algebra_desc)
    School.POWERS -> SchoolUi("🪨", R.string.school_powers, R.string.school_powers_desc)
    School.GEOMETRY -> SchoolUi("☀️", R.string.school_geometry, R.string.school_geometry_desc)
    School.SEQUENCES -> SchoolUi("🌙", R.string.school_sequences, R.string.school_sequences_desc)
}

/** The problem text to show: localised for word problems, the symbolic prompt otherwise. */
@Composable
fun problemText(problem: Problem): String {
    val key = problem.promptKey ?: return problem.prompt
    val res = geometryPrompts[key] ?: return problem.prompt
    return stringResource(res, *problem.promptArgs.toTypedArray())
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
