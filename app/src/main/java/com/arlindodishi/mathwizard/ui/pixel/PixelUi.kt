package com.arlindodishi.mathwizard.ui.pixel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import kotlin.math.floor
import kotlin.math.min

/**
 * Draws [sprite] as crisp squares: the pixel size is a whole number of screen pixels, so
 * edges never blur. [flash] (0..1) blends every colour towards white, for hit flashes.
 */
@Composable
fun PixelImage(sprite: PixelSprite, modifier: Modifier = Modifier, flash: Float = 0f, alpha: Float = 1f) {
    Canvas(modifier) {
        val px = floor(min(size.width / sprite.width, size.height / sprite.height)).coerceAtLeast(1f)
        val left = floor((size.width - px * sprite.width) / 2)
        val top = floor((size.height - px * sprite.height) / 2)
        for (run in sprite.runs) {
            val base = Color(run.argb)
            drawRect(
                color = if (flash > 0f) lerp(base, Color.White, flash.coerceIn(0f, 1f)) else base,
                topLeft = Offset(left + run.x * px, top + run.y * px),
                size = Size(run.length * px, px),
                alpha = alpha,
            )
        }
    }
}

/** Darker/lighter shades for bevels, computed in plain RGB like a hand-picked palette would be. */
fun Color.shade(amount: Float): Color = lerp(this, Color.Black, amount)
fun Color.tint(amount: Float): Color = lerp(this, Color.White, amount)

/**
 * Square button with an ink outline, a light top edge and a dark bottom edge; pressing it
 * moves the face down like a physical key.
 */
@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = SpireColors.Night,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val face = if (enabled) color else color.shade(0.45f)
    val edge = 5.dp
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .drawBehind {
                val ink = 3.dp.toPx()
                val bottom = (if (pressed) 2.dp else edge).toPx()
                drawRect(SpireColors.Ink)
                drawRect(face.shade(0.35f), topLeft = Offset(ink, ink), size = Size(size.width - 2 * ink, size.height - 2 * ink))
                drawRect(face, topLeft = Offset(ink, ink + (if (pressed) 3.dp.toPx() else 0f)), size = Size(size.width - 2 * ink, size.height - 2 * ink - bottom))
                drawRect(face.tint(0.3f), topLeft = Offset(ink, ink + (if (pressed) 3.dp.toPx() else 0f)), size = Size(size.width - 2 * ink, 3.dp.toPx()))
            }
            .padding(start = 12.dp, end = 12.dp, top = if (pressed) 6.dp else 3.dp, bottom = if (pressed) 3.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

/** Panel with a triple pixel border: ink, [accent], ink. */
fun Modifier.pixelPanel(accent: Color, fill: Color = SpireColors.Surface): Modifier = this
    .drawBehind {
        val b = 3.dp.toPx()
        drawRect(SpireColors.Ink)
        drawRect(accent, topLeft = Offset(b, b), size = Size(size.width - 2 * b, size.height - 2 * b))
        drawRect(SpireColors.Ink, topLeft = Offset(2 * b, 2 * b), size = Size(size.width - 4 * b, size.height - 4 * b))
        drawRect(fill, topLeft = Offset(3 * b, 3 * b), size = Size(size.width - 6 * b, size.height - 6 * b))
    }
    .padding(9.dp)

/** Bar of [segments] blocks; filled blocks round up so any HP left shows at least one. */
@Composable
fun SegmentedBar(fraction: Float, color: Color, modifier: Modifier = Modifier, segments: Int = 10, height: Dp = 10.dp) {
    val filled = if (fraction <= 0f) 0 else kotlin.math.ceil(fraction.coerceAtMost(1f) * segments).toInt()
    Row(
        modifier
            .height(height + 6.dp)
            .background(SpireColors.Ink)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(segments) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (i < filled) color else SpireColors.Track),
            )
        }
    }
}

/** A small icon followed by a number, e.g. the crystal balance. */
@Composable
fun IconValue(sprite: PixelSprite, value: String, color: Color, modifier: Modifier = Modifier, iconSize: Dp = 20.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PixelImage(sprite, Modifier.size(iconSize))
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, modifier = Modifier.offset(y = (-1).dp))
    }
}
