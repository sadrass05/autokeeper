package com.example.autobookkeeper.ui.components

import android.graphics.Color
import androidx.compose.animation.Crossfade
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.autobookkeeper.ui.viewmodel.DailyExpense
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

enum class ChartType { LINE, BAR }

private val barColors = listOf(
    ComposeColor(0xFFE8A87C),
    ComposeColor(0xFFB5735A),
    ComposeColor(0xFFD4956A),
    ComposeColor(0xFFC4815C),
    ComposeColor(0xFFE8C49A),
    ComposeColor(0xFFA0674A),
    ComposeColor(0xFFD4A574)
)

private fun lineDataEquals(old: LineData, new: LineData): Boolean {
    val oldSet = old.dataSets.firstOrNull() ?: return false
    val newSet = new.dataSets.firstOrNull() ?: return false
    if (oldSet.entryCount != newSet.entryCount) return false
    for (i in 0 until oldSet.entryCount) {
        if (oldSet.getEntryForIndex(i).y != newSet.getEntryForIndex(i).y) return false
        if (oldSet.getEntryForIndex(i).x != newSet.getEntryForIndex(i).x) return false
    }
    return true
}

private fun barDataEquals(old: BarData, new: BarData): Boolean {
    val oldSet = old.dataSets.firstOrNull() ?: return false
    val newSet = new.dataSets.firstOrNull() ?: return false
    if (oldSet.entryCount != newSet.entryCount) return false
    for (i in 0 until oldSet.entryCount) {
        if (oldSet.getEntryForIndex(i).y != newSet.getEntryForIndex(i).y) return false
        if (oldSet.getEntryForIndex(i).x != newSet.getEntryForIndex(i).x) return false
    }
    return true
}

@Composable
fun TrendChart(
    data: List<DailyExpense>,
    chartType: ChartType,
    modifier: Modifier = Modifier
) {
    val dateLabels = remember(data) { data.map { it.date } }
    val tealColor = ComposeColor(0xFF26A69A)
    val tealColorArgb = tealColor.toArgb()
    val fillColorArgb = tealColor.copy(alpha = 0.2f).toArgb()
    val onSurfaceArgb = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColorArgb = MaterialTheme.colorScheme.outlineVariant.toArgb()

    val lineEntries = remember(data) {
        data.mapIndexed { index, item -> Entry(index.toFloat(), item.amount) }
    }

    val barEntries = remember(data) {
        data.mapIndexed { index, item -> BarEntry(index.toFloat(), item.amount) }
    }

    Crossfade(targetState = chartType) { currentType ->
        when (currentType) {
            ChartType.LINE -> {
                AndroidView(
                    modifier = modifier.fillMaxWidth().height(140.dp),
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
                                setDrawLabels(true)
                                textColor = onSurfaceArgb
                                labelCount = dateLabels.size
                                granularity = 1f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                                        val index = value.toInt()
                                        return if (index in dateLabels.indices) dateLabels[index] else ""
                                    }
                                }
                            }

                            axisLeft.apply {
                                setDrawGridLines(true)
                                setDrawAxisLine(false)
                                setDrawLabels(false)
                                setDrawZeroLine(false)
                                axisMinimum = 0f
                                gridColor = gridColorArgb
                            }

                            axisRight.isEnabled = false

                            animateX(500)
                        }
                    },
                    update = { chart ->
                        chart.xAxis.textColor = onSurfaceArgb
                        chart.axisLeft.gridColor = gridColorArgb

                        val dataSet = LineDataSet(lineEntries, "").apply {
                            color = tealColorArgb
                            lineWidth = 2.5f
                            setDrawCircles(true)
                            circleRadius = 4f
                            setCircleColor(tealColorArgb)
                            circleHoleColor = tealColorArgb
                            setDrawValues(true)
                            valueTextColor = onSurfaceArgb
                            valueTextSize = 10f
                            valueFormatter = object : ValueFormatter() {
                                override fun getPointLabel(entry: Entry?): String {
                                    if (entry == null) return ""
                                    return "¥${"%.2f".format(entry.y)}"
                                }
                            }
                            setDrawFilled(true)
                            fillColor = fillColorArgb
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                        }

                        val newData = LineData(dataSet)
                        if (chart.data == null || !lineDataEquals(chart.data, newData)) {
                            chart.data = newData
                            chart.invalidate()
                        }
                    }
                )
            }

            ChartType.BAR -> {
                AndroidView(
                    modifier = modifier.fillMaxWidth().height(140.dp),
                    factory = { context ->
                        BarChart(context).apply {
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
                                setDrawLabels(true)
                                textColor = onSurfaceArgb
                                labelCount = dateLabels.size
                                granularity = 1f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                                        val index = value.toInt()
                                        return if (index in dateLabels.indices) dateLabels[index] else ""
                                    }
                                }
                            }

                            axisLeft.apply {
                                setDrawGridLines(true)
                                setDrawAxisLine(false)
                                setDrawLabels(false)
                                setDrawZeroLine(false)
                                axisMinimum = 0f
                                gridColor = gridColorArgb
                            }

                            axisRight.isEnabled = false

                            animateX(500)
                        }
                    },
                    update = { chart ->
                        chart.xAxis.textColor = onSurfaceArgb
                        chart.axisLeft.gridColor = gridColorArgb

                        val dataSet = BarDataSet(barEntries, "").apply {
                            colors = barColors.map { it.toArgb() }
                            setDrawValues(true)
                            valueTextColor = onSurfaceArgb
                            valueTextSize = 10f
                            valueFormatter = object : ValueFormatter() {
                                override fun getBarLabel(barEntry: BarEntry?): String {
                                    if (barEntry == null) return ""
                                    return "¥${"%.2f".format(barEntry.y)}"
                                }
                            }
                        }

                        val newData = BarData(dataSet)
                        newData.barWidth = 0.6f
                        if (chart.data == null || !barDataEquals(chart.data, newData)) {
                            chart.data = newData
                            chart.invalidate()
                        }
                    }
                )
            }
        }
    }
}
