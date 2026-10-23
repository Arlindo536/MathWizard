package com.arlindodishi.mathwizard.ui.pixel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

enum class SpellKind { HIT, CRITICAL, HURT }

/**
 * One-shot pixel particles, replayed whenever [trigger] changes. HIT/CRITICAL: a bolt of
 * squares flies from the problem card ([from]) to the enemy ([to]) and bursts there.
 * HURT: shards fly from the enemy down towards the player. Positions are fractions of the size.
 */
@Composable
fun SpellEffect(
    trigger: Int,
    kind: SpellKind?,
    color: Color,
    modifier: Modifier = Modifier,
    from: Offset = Offset(0.5f, 0.78f),
    to: Offset = Offset(0.5f, 0.36f),
) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(trigger, kind) {
        if (kind == null || trigger == 0) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(if (kind == SpellKind.HURT) 450 else 650, easing = LinearEasing))
    }
    val particles = remember(trigger) {
        val r = Random(trigger)
        List(if (kind == SpellKind.CRITICAL) 26 else 16) {
            Particle(angle = r.nextFloat() * 6.283f, speed = 0.5f + r.nextFloat(), jitter = r.nextFloat() - 0.5f, light = r.nextBoolean())
        }
    }
    Canvas(modifier) {
        val t = progress.value
        if (kind == null || t >= 1f) return@Canvas
        val vp = floor(3.dp.toPx()).coerceAtLeast(1f)
        val start = Offset(from.x * size.width, from.y * size.height)
        val end = Offset(to.x * size.width, to.y * size.height)
        fun square(p: Offset, s: Int, c: Color, alpha: Float) =
            drawRect(c, Offset(floor(p.x / vp) * vp, floor(p.y / vp) * vp), Size(s * vp, s * vp), alpha = alpha.coerceIn(0f, 1f))
        val bright = color.tint(0.5f)
        when (kind) {
            SpellKind.HIT, SpellKind.CRITICAL -> {
                if (t < 0.45f) {
                    val k = t / 0.45f
                    particles.forEachIndexed { i, p ->
                        val lag = (i % 5) * 0.06f
                        val kk = (k - lag).coerceIn(0f, 1f)
                        val pos = start + (end - start) * kk + Offset(p.jitter * 18 * vp * (1 - kk), 0f)
                        square(pos, if (i % 3 == 0) 2 else 1, if (p.light) bright else color, 1f)
                    }
                } else {
                    val k = (t - 0.45f) / 0.55f
                    val reach = (if (kind == SpellKind.CRITICAL) 34 else 24) * vp
                    particles.forEach { p ->
                        val pos = end + Offset(cos(p.angle), sin(p.angle)) * (reach * p.speed * k)
                        square(pos, if (p.light) 2 else 1, if (p.light) bright else color, 1 - k)
                    }
                }
            }
            SpellKind.HURT -> {
                particles.forEach { p ->
                    val spread = Offset(p.jitter * size.width * 0.6f, 0f)
                    val pos = end + (start - end + spread) * t + Offset(0f, sin(p.angle) * 6 * vp)
                    square(pos, 2, if (p.light) color else color.shade(0.3f), 1 - t)
                }
            }
        }
    }
}

private data class Particle(val angle: Float, val speed: Float, val jitter: Float, val light: Boolean)
