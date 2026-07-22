package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ElectricBlue
import com.example.util.FormatUtils
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerGauge(
    currentSpeedBps: Long,
    maxSpeedMbps: Float = 50f, // 50 MB/s gauge scale
    modifier: Modifier = Modifier
) {
    val speedMbps = currentSpeedBps / 1_000_000f
    val targetRatio = (speedMbps / maxSpeedMbps).coerceIn(0f, 1f)

    val animatedRatio by animateFloatAsState(
        targetValue = targetRatio,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "gaugeRatio"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("speedometer_gauge"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val strokeWidth = 22.dp.toPx()
            val radius = (canvasWidth - strokeWidth) / 2

            val startAngle = 140f
            val sweepAngle = 260f

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset((canvasWidth - radius * 2) / 2, (canvasHeight - radius * 2) / 2)
            )

            // Active Speed Arc Gradient
            val activeSweep = sweepAngle * animatedRatio
            if (activeSweep > 0) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(ElectricBlue, CyanPrimary, Color(0xFF00E676))
                    ),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset((canvasWidth - radius * 2) / 2, (canvasHeight - radius * 2) / 2)
                )
            }

            // Needle Tip Indicator
            val currentAngleRad = Math.toRadians((startAngle + activeSweep).toDouble())
            val needleLength = radius - 10.dp.toPx()
            val center = Offset(canvasWidth / 2, canvasHeight / 2)
            val needleEnd = Offset(
                x = center.x + needleLength * cos(currentAngleRad).toFloat(),
                y = center.y + needleLength * sin(currentAngleRad).toFloat()
            )

            drawCircle(
                color = CyanPrimary,
                radius = 6.dp.toPx(),
                center = needleEnd
            )
        }

        // Center Speed Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "LIVE SPEED",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = FormatUtils.formatSpeed(currentSpeedBps),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${activeClientsText(currentSpeedBps)} Bandwidth",
                style = MaterialTheme.typography.bodySmall,
                color = CyanPrimary
            )
        }
    }
}

private fun activeClientsText(speedBps: Long): String {
    return when {
        speedBps > 10_000_000 -> "Peak Heavy"
        speedBps > 3_000_000 -> "Active High"
        speedBps > 500_000 -> "Normal"
        else -> "Idle Low"
    }
}
