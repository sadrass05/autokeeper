package com.example.autobookkeeper.ui.components

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

@Composable
fun MiniLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    dateLabels: List<String> = emptyList()
) {
    val showLabels = dateLabels.isNotEmpty()
    val chartTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val chartLineColor = MaterialTheme.colorScheme.primary.toArgb()
    val chartFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f).toArgb()
    val chartGridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()

    val entries = remember(values) {
        values.mapIndexed { index, value -> Entry(index.toFloat(), value) }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth().height(120.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(false)
                setPinchZoom(false)
                isDoubleTapToZoomEnabled = false
                setDrawGridBackground(false)
                setBackgroundColor(Color.TRANSPARENT)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                    setDrawLabels(showLabels)
                    if (showLabels) {
                        labelCount = dateLabels.size
                        granularity = 1f
                        valueFormatter = object : ValueFormatter() {
                            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                                val index = value.toInt()
                                return if (index >= 0 && index < dateLabels.size) dateLabels[index] else ""
                            }
                        }
                    }
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    setDrawAxisLine(false)
                    setDrawLabels(false)
                    setDrawZeroLine(false)
                    axisMinimum = 0f
                }

                axisRight.apply {
                    isEnabled = false
                }

                animateX(500)
            }
        },
        update = { chart ->
            chart.xAxis.textColor = chartTextColor
            chart.axisLeft.gridColor = chartGridColor

            val dataSet = LineDataSet(entries, "").apply {
                color = chartLineColor
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = chartFillColor
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}