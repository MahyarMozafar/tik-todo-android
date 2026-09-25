package com.mahyarmozafar.tik.ui.components

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.mahyarmozafar.tik.ui.theme.SystemColor
import com.mahyarmozafar.tik.ui.theme.TikTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.random.Random

private class Piece(
    val startX: Float,
    val velocityX: Float,
    val velocityY: Float,
    val spin: Float,
    val flutter: Float,
    val size: Size,
    val color: Color,
    val round: Boolean,
)

private const val DURATION = 3.4f
private const val GRAVITY = 620f

/**
 * A short burst of confetti from both bottom corners. Change [trigger] to fire a new burst.
 * Nothing happens when animations are turned off in the phone's settings.
 */
@Composable
fun Confetti(trigger: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val colors = TikTheme.colors
    var pieces by remember { mutableStateOf<List<Piece>>(emptyList()) }
    var elapsed by remember { mutableLongStateOf(0L) }
    val first = remember { booleanArrayOf(true) }

    LaunchedEffect(trigger) {
        if (first[0]) {
            first[0] = false
            return@LaunchedEffect
        }
        val animationsOff = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        if (animationsOff) return@LaunchedEffect

        val palette = listOf(
            colors.accent, colors.partner,
            SystemColor.Yellow.color(colors.dark), SystemColor.Pink.color(colors.dark),
            SystemColor.Mint.color(colors.dark), SystemColor.Orange.color(colors.dark),
            SystemColor.Purple.color(colors.dark),
        )
        pieces = buildList {
            for (side in listOf(0f, 1f)) {
                val direction = if (side == 0f) 1f else -1f
                repeat(70) {
                    val round = Random.nextFloat() < 0.3f
                    val width = Random.nextFloat() * 3f + 6f
                    add(
                        Piece(
                            startX = side,
                            velocityX = direction * (Random.nextFloat() * 320f + 60f),
                            velocityY = -(Random.nextFloat() * 360f + 820f),
                            spin = Random.nextFloat() * 12f - 6f,
                            flutter = Random.nextFloat() * 2.5f + 1.5f,
                            size = Size(width, if (round) width else width * 1.7f),
                            color = palette.random(),
                            round = round,
                        ),
                    )
                }
            }
        }
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            elapsed = now - start
            if (elapsed / 1e9f > DURATION) break
        }
        pieces = emptyList()
    }

    if (pieces.isEmpty()) return
    Canvas(modifier.fillMaxSize().clearAndSetSemantics { }) {
        val time = elapsed / 1e9f
        val fade = ((DURATION - time) / 0.8f).coerceIn(0f, 1f)
        // The iOS numbers are in points; multiply by density to get the same motion here.
        for (piece in pieces) {
            val x = piece.startX * size.width + piece.velocityX * density * time
            val y = size.height + 20 * density + (piece.velocityY * time + 0.5f * GRAVITY * time * time) * density
            if (y > size.height + 40 * density) continue
            translate(x, y) {
                rotateRad(piece.spin * time, pivot = Offset.Zero) {
                    scale(scaleX = cos(piece.flutter * time * PI.toFloat()), scaleY = 1f, pivot = Offset.Zero) {
                        val w = piece.size.width * density
                        val h = piece.size.height * density
                        if (piece.round) {
                            drawOval(piece.color.copy(alpha = fade), topLeft = Offset(-w / 2, -h / 2), size = Size(w, h))
                        } else {
                            drawRoundRect(
                                piece.color.copy(alpha = fade),
                                topLeft = Offset(-w / 2, -h / 2),
                                size = Size(w, h),
                                cornerRadius = CornerRadius(1.5f * density),
                            )
                        }
                    }
                }
            }
        }
    }
}
