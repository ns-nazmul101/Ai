package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.model.AiStatus
import com.example.ui.theme.DevilAmber
import com.example.ui.theme.DevilCyan
import com.example.ui.theme.DevilGreen
import com.example.ui.theme.DevilRed

@Composable
fun DevilAvatar(
    status: AiStatus,
    rmsDb: Float,
    isBengali: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_anim")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val dynamicWaveScale = (1f + (rmsDb * 0.4f)).coerceIn(1f, 1.45f)
    val effectiveScale = if (status == AiStatus.LISTENING) dynamicWaveScale else pulseScale

    Column(
        modifier = modifier.testTag("devil_avatar_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background ambient glow rings
            Canvas(modifier = Modifier.size(105.dp).scale(effectiveScale)) {
                val ringColor = when (status) {
                    AiStatus.LISTENING -> DevilCyan.copy(alpha = 0.45f)
                    AiStatus.THINKING -> DevilAmber.copy(alpha = 0.45f)
                    AiStatus.EXECUTING -> DevilRed.copy(alpha = 0.5f)
                    AiStatus.COMPLETED -> DevilGreen.copy(alpha = 0.45f)
                    else -> DevilRed.copy(alpha = 0.25f)
                }

                // Outer animated ring
                drawCircle(
                    color = ringColor,
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Inner sonic pulse ring
                drawCircle(
                    color = ringColor.copy(alpha = 0.2f),
                    radius = (size.minDimension / 2f) * 0.85f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Core Avatar Orb
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                when (status) {
                                    AiStatus.LISTENING -> DevilCyan
                                    AiStatus.THINKING -> DevilAmber
                                    AiStatus.EXECUTING -> DevilRed
                                    AiStatus.COMPLETED -> DevilGreen
                                    else -> DevilRed
                                },
                                Color(0xFF1E0A16),
                                Color(0xFF0B0D14)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(DevilRed, DevilCyan, DevilRed)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Cyber Devil Face Canvas (Horns + Glowing Eyes)
                Canvas(modifier = Modifier.size(54.dp)) {
                    val w = size.width
                    val h = size.height

                    // Left Horn
                    val leftHorn = Path().apply {
                        moveTo(w * 0.30f, h * 0.35f)
                        quadraticTo(w * 0.22f, h * 0.15f, w * 0.32f, h * 0.10f)
                        quadraticTo(w * 0.38f, h * 0.22f, w * 0.38f, h * 0.35f)
                        close()
                    }
                    drawPath(leftHorn, color = DevilRed)

                    // Right Horn
                    val rightHorn = Path().apply {
                        moveTo(w * 0.70f, h * 0.35f)
                        quadraticTo(w * 0.78f, h * 0.15f, w * 0.68f, h * 0.10f)
                        quadraticTo(w * 0.62f, h * 0.22f, w * 0.62f, h * 0.35f)
                        close()
                    }
                    drawPath(rightHorn, color = DevilRed)

                    // Eyes
                    val eyeColor = when (status) {
                        AiStatus.LISTENING -> Color.White
                        AiStatus.THINKING -> DevilAmber
                        else -> DevilCyan
                    }

                    // Left Slanted Cyber Eye
                    val leftEye = Path().apply {
                        moveTo(w * 0.33f, h * 0.50f)
                        lineTo(w * 0.45f, h * 0.55f)
                        lineTo(w * 0.37f, h * 0.59f)
                        close()
                    }
                    drawPath(leftEye, color = eyeColor)

                    // Right Slanted Cyber Eye
                    val rightEye = Path().apply {
                        moveTo(w * 0.67f, h * 0.50f)
                        lineTo(w * 0.55f, h * 0.55f)
                        lineTo(w * 0.63f, h * 0.59f)
                        close()
                    }
                    drawPath(rightEye, color = eyeColor)

                    // Voice Frequency Bars (Mouth Area)
                    val barColor = if (status == AiStatus.LISTENING) DevilCyan else Color.White.copy(alpha = 0.8f)
                    val midY = h * 0.72f
                    drawLine(
                        color = barColor,
                        start = Offset(w * 0.40f, midY - 2.dp.toPx()),
                        end = Offset(w * 0.40f, midY + 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = barColor,
                        start = Offset(w * 0.50f, midY - 5.dp.toPx()),
                        end = Offset(w * 0.50f, midY + 5.dp.toPx()),
                        strokeWidth = 2.5.dp.toPx()
                    )
                    drawLine(
                        color = barColor,
                        start = Offset(w * 0.60f, midY - 2.dp.toPx()),
                        end = Offset(w * 0.60f, midY + 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Status pill
        val statusText = if (isBengali) status.labelBn else status.labelEn
        val statusDotColor = when (status) {
            AiStatus.LISTENING -> DevilCyan
            AiStatus.THINKING -> DevilAmber
            AiStatus.EXECUTING -> DevilRed
            AiStatus.COMPLETED -> DevilGreen
            AiStatus.ERROR -> Color.Red
            else -> DevilCyan
        }

        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusDotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
