package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DangerRed

@Composable
fun TrafficChart(
    rxPoints: List<Float>, // MB/s
    txPoints: List<Float>, // MB/s
    modifier: Modifier = Modifier
) {
    val maxPoint = ((rxPoints.maxOrNull() ?: 1f).coerceAtLeast(txPoints.maxOrNull() ?: 1f) * 1.25f).coerceAtLeast(2f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .testTag("traffic_chart")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Real-Time Throughput",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Legend RX
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Download",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Legend TX
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(DangerRed)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Upload",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val width = size.width
            val height = size.height

            // Grid Lines
            val gridStep = height / 3
            for (i in 0..3) {
                val y = i * gridStep
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (rxPoints.size >= 2) {
                val stepX = width / (rxPoints.size - 1)

                // RX Path (Download)
                val rxPath = Path()
                rxPoints.forEachIndexed { i, valMb ->
                    val x = i * stepX
                    val y = height - ((valMb / maxPoint) * height).coerceIn(0f, height)
                    if (i == 0) rxPath.moveTo(x, y) else rxPath.lineTo(x, y)
                }

                drawPath(
                    path = rxPath,
                    color = CyanPrimary,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // RX Fill Gradient
                val rxFillPath = Path().apply {
                    addPath(rxPath)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(
                    path = rxFillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(CyanPrimary.copy(alpha = 0.25f), Color.Transparent)
                    )
                )

                // TX Path (Upload)
                val txPath = Path()
                txPoints.forEachIndexed { i, valMb ->
                    val x = i * stepX
                    val y = height - ((valMb / maxPoint) * height).coerceIn(0f, height)
                    if (i == 0) txPath.moveTo(x, y) else txPath.lineTo(x, y)
                }

                drawPath(
                    path = txPath,
                    color = DangerRed,
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }
        }
    }
}
