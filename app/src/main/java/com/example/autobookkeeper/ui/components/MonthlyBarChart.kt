package com.example.autobookkeeper.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.autobookkeeper.ui.viewmodel.MonthlyExpense
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.example.autobookkeeper.R

private class RoundedBarChartRenderer(
    chart: BarChart,
    animator: com.github.mikephil.charting.animation.ChartAnimator,
    viewPortHandler: ViewPortHandler
) : BarChartRenderer(chart, animator, viewPortHandler) {

    private val radius = 8f

    override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
        val transformer = mChart.getTransformer(dataSet.axisDependency)
        mBarBorderPaint.color = dataSet.barBorderColor
        mBarBorderPaint.strokeWidth = dataSet.barBorderWidth.toFloat()
        val drawBorder = dataSet.barBorderWidth > 0f

        val phaseX = mAnimator.phaseX
        val phaseY = mAnimator.phaseY

        if (mChart.isDrawBarShadowEnabled) {
            mShadowPaint.color = dataSet.barShadowColor
            val barData = mChart.barData
            val barWidth = barData.barWidth
            val barWidthHalf = barWidth / 2f
            var x = 0f
            var i = 0
            val count = Math.min(Math.ceil((dataSet.entryCount * phaseX).toDouble()).toInt(), dataSet.entryCount)
            while (i < count) {
                val e = dataSet.getEntryForIndex(i)
                x = e.x
                val left = x - barWidthHalf
                val right = x + barWidthHalf
                val top: Float
                val bottom: Float
                if (mChart.isInverted(dataSet.axisDependency)) {
                    bottom = 0f
                    top = e.y * phaseY
                } else {
                    bottom = e.y * phaseY
                    top = 0f
                }
                c.drawRect(left, top, right, bottom, mShadowPaint)
                i++
            }
        }

        val buffer = mBarBuffers[index]
        buffer.setPhases(phaseX, phaseY)
        buffer.setDataSet(index)
        buffer.setInverted(mChart.isInverted(dataSet.axisDependency))
        buffer.setBarWidth(mChart.barData.barWidth)
        buffer.feed(dataSet)
        transformer.pointValuesToPixel(buffer.buffer)

        val isSingleColor = dataSet.colors.size == 1
        if (isSingleColor) {
            mRenderPaint.color = dataSet.color
        }

        var j = 0
        while (j < buffer.size()) {
            if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2])) {
                j += 4
                continue
            }
            if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j])) {
                break
            }

            if (!isSingleColor) {
                mRenderPaint.color = dataSet.getColor(j / 4)
            }

            val left = buffer.buffer[j]
            val top = buffer.buffer[j + 1]
            val right = buffer.buffer[j + 2]
            val bottom = buffer.buffer[j + 3]

            val rect = if (top < bottom) {
                RectF(left, top, right, bottom)
            } else {
                RectF(left, bottom, right, top)
            }
            c.drawRoundRect(rect, radius, radius, mRenderPaint)

            if (drawBorder) {
                c.drawRoundRect(rect, radius, radius, mBarBorderPaint)
            }

            j += 4
        }
    }
}

private class MonthlyMarkerView(
    context: Context,
    layoutResource: Int
) : com.github.mikephil.charting.components.MarkerView(context, layoutResource) {

    private val tvContent: TextView
    private val padding = 16
    private val radius = 12f

    init {
        tvContent = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(padding, padding / 2, padding, padding / 2)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC333333"))
                cornerRadius = radius
            }
        }
        this.addView(tvContent, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    override fun refreshContent(e: com.github.mikephil.charting.data.Entry?, highlight: Highlight?) {
        val expense = e?.data as? MonthlyExpense
        if (expense != null) {
            tvContent.text = "${expense.month}月共支出 ¥${"%.2f".format(expense.amount)}"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-width / 2).toFloat(), (-height - 20).toFloat())
    }
}

private val monthlyBarColors = listOf(
    AndroidColor.parseColor("#FFE8A87C"),
    AndroidColor.parseColor("#FFFB5735"),
    AndroidColor.parseColor("#FFD4956A"),
    AndroidColor.parseColor("#FFC4815C"),
    AndroidColor.parseColor("#FFE8C49A"),
    AndroidColor.parseColor("#FFA0674A"),
    AndroidColor.parseColor("#FFD4A574")
)

@Composable
fun MonthlyBarChart(
    data: List<MonthlyExpense>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f).toArgb()

    val chartDataKey = remember(data) {
        data.joinToString { "${it.year}-${it.month}-${it.amount}" }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawBarShadow(false)
                setDrawValueAboveBar(true)
                setFitBars(true)
                setBackgroundColor(Color.TRANSPARENT)
                setDrawGridBackground(false)
                setTouchEnabled(true)
                isDoubleTapToZoomEnabled = false
                setPinchZoom(false)
                setScaleEnabled(false)
                setVisibleXRangeMaximum(6f)

                renderer = RoundedBarChartRenderer(this, animator, viewPortHandler)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                    granularity = 1f
                    textColor = axisColor
                    textSize = 10f
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = axisColor
                    gridLineWidth = 0.5f
                    setDrawAxisLine(false)
                    setDrawLabels(false)
                    axisMinimum = 0f
                }

                axisRight.isEnabled = false

                marker = MonthlyMarkerView(ctx, R.layout.marker_empty)

                setNoDataText("")
            }
        },
        update = { chart ->
            if (data.isEmpty()) {
                chart.clear()
                chart.invalidate()
                return@AndroidView
            }

            val entries = data.mapIndexed { index, item ->
                BarEntry(index.toFloat(), item.amount, item)
            }

            val dataSet = BarDataSet(entries, "").apply {
                colors = monthlyBarColors
                valueTextSize = 10f
                setDrawValues(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getBarLabel(entry: BarEntry?): String {
                        val amount = entry?.y ?: 0f
                        return "¥${"%.2f".format(amount)}"
                    }
                }
            }

            chart.data = BarData(dataSet)
            chart.data.barWidth = 0.5f

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index in data.indices) data[index].label.replace("\n", " ") else ""
                }
            }

            chart.animateY(500)
            chart.invalidate()
        }
    )
}