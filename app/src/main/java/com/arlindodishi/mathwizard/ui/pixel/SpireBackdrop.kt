package com.arlindodishi.mathwizard.ui.pixel

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import kotlin.math.floor
import kotlin.random.Random

/**
 * Pixel-art night sky inside the spire: twinkling stars, a moon, distant towers and stone
 * pillars with torches on both sides. [climb] (in floors) scrolls the pillars down, so moving
 * up a floor looks like climbing. Everything snaps to a 3 dp "virtual pixel" grid.
 */
@Composable
fun SpireBackdrop(climb: Float, torch: Color, modifier: Modifier = Modifier) {
    val twinkle = rememberInfiniteTransition(label = "twinkle")
    val phase by twinkle.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    val stars = remember {
        val r = Random(7)
        List(46) { Triple(r.nextFloat(), r.nextFloat() * 0.62f, r.nextInt(5)) }
    }
    Canvas(modifier) {
        val vp = floor(3.dp.toPx()).coerceAtLeast(1f)
        fun cell(x: Float, y: Float, w: Float, h: Float, color: Color) =
            drawRect(color, Offset(floor(x / vp) * vp, floor(y / vp) * vp), Size(w * vp, h * vp))

        // Sky in flat bands.
        val bands = listOf(Color(0xFF110A22), Color(0xFF140C2A), Color(0xFF170E30), Color(0xFF1B1038))
        bands.forEachIndexed { i, c -> drawRect(c, Offset(0f, size.height * i / bands.size), Size(size.width, size.height / bands.size + 1)) }

        val flicker = (phase * 5).toInt()
        stars.forEachIndexed { i, (sx, sy, group) ->
            val dim = (group + flicker) % 5 == 0
            cell(sx * size.width, sy * size.height, 1f, 1f, if (dim) Color(0xFF6B63A8) else Color(0xFFD9D4FF))
        }
        drawMoon(size.width * 0.80f, size.height * 0.29f, vp)
        drawTowers(vp, Color(0xFF1E1440), seed = 3, heightFactor = 0.30f)
        drawTowers(vp, Color(0xFF261A50), seed = 11, heightFactor = 0.20f)
        drawPillar(left = 0f, climb = climb, vp = vp, torch = torch, phase = phase)
        drawPillar(left = size.width - 10 * vp, climb = climb, vp = vp, torch = torch, phase = phase, offsetRows = 2)
    }
}

private fun DrawScope.drawMoon(cx: Float, cy: Float, vp: Float) {
    val rows = listOf("..XXXX..", ".XXXXXX.", "XXXXXX..", "XXXXX...", "XXXXX...", "XXXXXX..", ".XXXXXX.", "..XXXX..")
    rows.forEachIndexed { y, row ->
        row.forEachIndexed { x, c ->
            if (c == 'X') drawRect(Color(0xFFFFE8A8), Offset(floor(cx / vp) * vp + x * vp, floor(cy / vp) * vp + y * vp), Size(vp, vp))
        }
    }
}

/** A row of flat towers with stepped tops along the bottom of the screen. */
private fun DrawScope.drawTowers(vp: Float, color: Color, seed: Int, heightFactor: Float) {
    val r = Random(seed)
    val baseY = size.height * (1 - heightFactor * 0.5f)
    var x = 0f
    while (x < size.width) {
        val w = (6 + r.nextInt(8)) * vp
        val h = size.height * heightFactor * (0.4f + r.nextFloat() * 0.6f)
        val top = floor((baseY - h) / vp) * vp
        drawRect(color, Offset(x, top), Size(w, size.height - top))
        drawRect(color, Offset(x + w / 2 - vp, top - 4 * vp), Size(2 * vp, 4 * vp))
        if (r.nextBoolean()) drawRect(SpireColors.Gold.copy(alpha = 0.7f), Offset(x + w / 2 - vp / 2, top + 6 * vp), Size(vp, vp))
        x += w + (2 + r.nextInt(6)) * vp
    }
    drawRect(color, Offset(0f, baseY), Size(size.width, size.height - baseY))
}

/** A 10-cell-wide brick pillar that scrolls with [climb]; a torch every 24 rows. */
private fun DrawScope.drawPillar(left: Float, climb: Float, vp: Float, torch: Color, phase: Float, offsetRows: Int = 0) {
    val brickH = 4 * vp
    val period = 24 * brickH
    val scroll = (climb * 8 * brickH) % period
    val stone = Color(0xFF2E2350)
    val stoneLight = Color(0xFF3A2D63)
    val mortar = Color(0xFF1A1233)
    drawRect(mortar, Offset(left, 0f), Size(10 * vp, size.height))
    var y = -period + scroll
    var row = offsetRows
    while (y < size.height) {
        val shift = if (row % 2 == 0) 0f else 2 * vp
        var x = left - shift
        while (x < left + 10 * vp) {
            val bx = x.coerceAtLeast(left)
            val bw = (x + 4 * vp).coerceAtMost(left + 10 * vp) - bx - vp
            if (bw > 0) {
                drawRect(stone, Offset(bx, y), Size(bw, brickH - vp))
                drawRect(stoneLight, Offset(bx, y), Size(bw, vp))
            }
            x += 4 * vp
        }
        if (row % 24 == 12) {
            val tall = if ((phase * 6).toInt() % 2 == 0) 3 else 2
            drawRect(Color(0xFF6B4F2A), Offset(left + 4 * vp, y + vp), Size(2 * vp, 3 * vp))
            drawRect(torch, Offset(left + 4 * vp, y - tall * vp), Size(2 * vp, tall * vp + vp))
            drawRect(SpireColors.Gold, Offset(left + 4 * vp, y - vp), Size(vp, vp))
        }
        y += brickH
        row++
    }
}
