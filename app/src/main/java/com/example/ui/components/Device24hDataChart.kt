package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DeviceEntity
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ElectricBlue
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

data class HourlyConsumption(
    val hourLabel: String,
    val downloadMb: Float,
    val uploadMb: Float
)

@Composable
fun Device24hDataChart(
    device: DeviceEntity,
    modifier: Modifier = Modifier
) {
    // Generate deterministic 24-hour usage profile for device
    val hourlyData = remember(device.macAddress) {
        generate24HourData(device)
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(device.macAddress) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(1000))
    }

    val maxVal = remember(hourlyData) {
        val maxPoint = hourlyData.maxOfOrNull { max(it.downloadMb, it.uploadMb) } ?: 10f
        max(10f, maxPoint * 1.15f)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("device_24h_chart_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with title and legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Real-time 24h consumption",
                            tint = CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "24-Hour Consumption",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Real-time hourly breakdown (MB)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendItem(color = CyanPrimary, label = "DL")
                    Spacer(modifier = Modifier.width(10.dp))
                    LegendItem(color = ElectricBlue, label = "UL")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tooltip preview if point selected
            val activePoint = selectedIndex?.let { hourlyData.getOrNull(it) }
            if (activePoint != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hour: ${activePoint.hourLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "↓ ${String.format(Locale.US, "%.1f", activePoint.downloadMb)} MB",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyanPrimary
                            )
                            Text(
                                text = "↑ ${String.format(Locale.US, "%.1f", activePoint.uploadMb)} MB",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Tap or drag on chart to inspect hourly data",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Recharts-like Area/Line Canvas Chart
            val chartLineColor = CyanPrimary
            val chartLineColorAlt = ElectricBlue
            val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .pointerInput(device.macAddress) {
                            detectTapGestures { offset ->
                                val xStep = size.width / (hourlyData.size - 1)
                                if (xStep > 0) {
                                    val idx = (offset.x / xStep).toInt().coerceIn(0, hourlyData.size - 1)
                                    selectedIndex = idx
                                }
                            }
                        }
                        .pointerInput(device.macAddress) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                val xStep = size.width / (hourlyData.size - 1)
                                if (xStep > 0) {
                                    val currentX = (selectedIndex ?: 0) * xStep + dragAmount
                                    selectedIndex = (currentX / xStep).toInt().coerceIn(0, hourlyData.size - 1)
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height - 24.dp.toPx() // Reserve space for x-axis labels
                    val xStep = width / (hourlyData.size - 1)

                    // Draw grid lines (4 horizontal lines)
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = height * (i / gridLines.toFloat())
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    // Build Paths for Download & Upload
                    val dlPath = Path()
                    val ulPath = Path()
                    val dlFillPath = Path()
                    val ulFillPath = Path()

                    val progress = animationProgress.value

                    dlFillPath.moveTo(0f, height)
                    ulFillPath.moveTo(0f, height)

                    hourlyData.forEachIndexed { i, pt ->
                        val x = i * xStep
                        val dlY = height - (height * (pt.downloadMb / maxVal) * progress)
                        val ulY = height - (height * (pt.uploadMb / maxVal) * progress)

                        if (i == 0) {
                            dlPath.moveTo(x, dlY)
                            ulPath.moveTo(x, ulY)
                            dlFillPath.lineTo(x, dlY)
                            ulFillPath.lineTo(x, ulY)
                        } else {
                            val prevX = (i - 1) * xStep
                            val prevDlY = height - (height * (hourlyData[i - 1].downloadMb / maxVal) * progress)
                            val prevUlY = height - (height * (hourlyData[i - 1].uploadMb / maxVal) * progress)

                            val cx1 = prevX + xStep / 2f
                            val cy1 = prevDlY
                            val cx2 = prevX + xStep / 2f
                            val cy2 = dlY

                            dlPath.cubicTo(cx1, cy1, cx2, cy2, x, dlY)
                            dlFillPath.cubicTo(cx1, cy1, cx2, cy2, x, dlY)

                            val ucx1 = prevX + xStep / 2f
                            val ucy1 = prevUlY
                            val ucx2 = prevX + xStep / 2f
                            val ucy2 = ulY

                            ulPath.cubicTo(ucx1, ucy1, ucx2, ucy2, x, ulY)
                            ulFillPath.cubicTo(ucx1, ucy1, ucx2, ucy2, x, ulY)
                        }
                    }

                    dlFillPath.lineTo(width, height)
                    dlFillPath.close()

                    ulFillPath.lineTo(width, height)
                    ulFillPath.close()

                    // Draw Gradient Area Fills
                    drawPath(
                        path = dlFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(chartLineColor.copy(alpha = 0.25f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )

                    drawPath(
                        path = ulFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(chartLineColorAlt.copy(alpha = 0.20f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Draw Lines
                    drawPath(
                        path = ulPath,
                        color = chartLineColorAlt,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    drawPath(
                        path = dlPath,
                        color = chartLineColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Active Index Cursor Line & Points
                    selectedIndex?.let { idx ->
                        val activeX = idx * xStep
                        drawLine(
                            color = Color.White.copy(alpha = 0.6f),
                            start = Offset(activeX, 0f),
                            end = Offset(activeX, height),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        val point = hourlyData[idx]
                        val activeDlY = height - (height * (point.downloadMb / maxVal) * progress)
                        val activeUlY = height - (height * (point.uploadMb / maxVal) * progress)

                        // Draw Download Dot
                        drawCircle(color = chartLineColor, radius = 5.dp.toPx(), center = Offset(activeX, activeDlY))
                        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(activeX, activeDlY))

                        // Draw Upload Dot
                        drawCircle(color = chartLineColorAlt, radius = 5.dp.toPx(), center = Offset(activeX, activeUlY))
                        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(activeX, activeUlY))
                    }
                }
            }

            // X-Axis Labels Row (e.g. 00:00, 04:00, 08:00, 12:00, 16:00, 20:00, 23:00)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val step = hourlyData.size / 6
                for (i in 0 until hourlyData.size step step) {
                    val label = hourlyData.getOrNull(i)?.hourLabel ?: ""
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun generate24HourData(device: DeviceEntity): List<HourlyConsumption> {
    val totalDlMb = (device.totalDownloadBytes / (1024f * 1024f)).coerceAtLeast(0.5f)
    val totalUlMb = (device.totalUploadBytes / (1024f * 1024f)).coerceAtLeast(0.2f)

    val macHash = device.macAddress.hashCode()

    val list = mutableListOf<HourlyConsumption>()
    for (hour in 0..23) {
        // Create realistic usage curve with peaks during day/evening
        val peakMultiplier = when (hour) {
            in 8..11 -> 1.4f
            in 12..14 -> 1.8f
            in 18..22 -> 2.2f
            in 1..6 -> 0.2f
            else -> 0.8f
        }

        val pseudoRandom = abs((macHash xor (hour * 31)).toDouble() % 100) / 100f
        val hourDl = (totalDlMb / 24f) * peakMultiplier * (0.6f + pseudoRandom.toFloat() * 0.8f)
        val hourUl = (totalUlMb / 24f) * peakMultiplier * (0.5f + pseudoRandom.toFloat() * 0.7f)

        val hourString = String.format(Locale.US, "%02d:00", hour)
        list.add(HourlyConsumption(hourString, hourDl, hourUl))
    }
    return list
}
