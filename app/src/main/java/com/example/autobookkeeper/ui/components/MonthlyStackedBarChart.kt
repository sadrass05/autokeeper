package com.example.autobookkeeper.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.autobookkeeper.ui.viewmodel.MonthlyExpense
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.highlight.Highlight
import android.graphics.Color as AndroidColor

@Composable
fun MonthlyStackedBarChart(
    data: List<MonthlyExpense>,
    modifier: Modifier = Modifier
) {
    val nonFinanceColor = MaterialTheme.colorScheme.primary.toArgb()
    val financeColor = MaterialTheme.colorScheme.tertiary.toArgb()
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f).toArgb()
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1E1E1E).toArgb() else AndroidColor.WHITE

    val chartDataKey = remember(data) { data.hashCode() }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        factory = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                setDrawGridBackground(false)
                setBackgroundColor(bgColor)
                setDrawBarShadow(false)
                setHighlightFullBarEnabled(false)
                isHighlightPerDragEnabled = false
                setTouchEnabled(true)
                isDoubleTapToZoomEnabled = false
                setPinchZoom(false)
                setScaleEnabled(false)
                setVisibleXRangeMaximum(6f)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    textColor = labelColor
                    textSize = 10f
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = axisColor
                    textColor = labelColor
                    textSize = 9f
                    axisMinimum = 0f
                }
                axisRight.isEnabled = false

                legend.apply {
                    isEnabled = true
                    textColor = labelColor
                    textSize = 10f
                    form = Legend.LegendForm.SQUARE
                    formSize = 10f
                    orientation = Legend.LegendOrientation.HORIZONTAL
                    horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                    verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                }

                animateY(600)

                val markerView = MonthlyDetailMarkerView(ctx, isStacked = true)
                marker = markerView

                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {}
                    override fun onNothingSelected() {}
                })
            }
        },
        update = { chart ->
            if (data.isEmpty()) {
                chart.clear()
                chart.invalidate()
                return@AndroidView
            }

            val entries = data.mapIndexed { index, item ->
                BarEntry(
                    index.toFloat(),
                    floatArrayOf(
                        item.nonFinanceAmount.toFloat(),
                        item.financeAmount.toFloat().coerceAtLeast(0f)
                    )
                )
            }

            val dataSet = BarDataSet(entries, "").apply {
                colors = listOf(nonFinanceColor, financeColor)
                stackLabels = arrayOf("日常支出", "理财支出")
                setDrawValues(true)
                valueTextSize = 9f
                valueTextColor = labelColor
                valueFormatter = object : ValueFormatter() {
                    override fun getBarStackedLabel(value: Float, stackedEntry: BarEntry?): String {
                        return if (value > 0.01f) "¥${"%.0f".format(value)}" else ""
                    }
                }
            }

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(
                data.map { it.label }.toTypedArray()
            )

            val barData = BarData(dataSet).apply {
                barWidth = 0.6f
            }

            chart.data = barData
            (chart.marker as? MonthlyDetailMarkerView)?.monthlyData = data
            chart.invalidate()
        }
    )
}