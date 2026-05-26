package com.example.autobookkeeper.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import kotlinx.coroutines.tasks.await
class OcrManager @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(bitmap: Bitmap): String {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun parseFinanceData(text: String): FinanceData {
        val buyAmount = parseAmount(text, "买入|投入|本金")
        val currentValue = parseAmount(text, "市值|估值|当前")
        val profit = parseAmount(text, "收益|盈利")
        val profitRate = parseRate(text)

        return FinanceData(
            buyAmount = buyAmount,
            currentValue = currentValue,
            profit = profit,
            profitRate = profitRate
        )
    }

    private fun parseAmount(text: String, keywords: String): Double {
        val pattern = "$keywords[^0-9]*([0-9]*\\.?[0-9]+)".toRegex(RegexOption.IGNORE_CASE)
        return pattern.find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    private fun parseRate(text: String): Double {
        val pattern = "([0-9]+\\.?[0-9]*)%".toRegex()
        return pattern.find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }
}

data class FinanceData(
    val buyAmount: Double,
    val currentValue: Double,
    val profit: Double,
    val profitRate: Double
)