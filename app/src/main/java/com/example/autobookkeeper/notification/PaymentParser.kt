package com.example.autobookkeeper.notification

import com.example.autobookkeeper.data.entity.ExpenseRecord
import javax.inject.Inject

class PaymentParser @Inject constructor() {

    private val wechatPackage = "com.tencent.mm"
    private val alipayPackage = "com.eg.android.AlipayGphone"
    private val pinduoduoPackage = "com.xunmeng.pinduoduo"

    private val paymentKeywords = listOf("支付", "付款", "消费", "扣费", "扣款", "交易", "买单", "缴费")
    private val amountPatterns = listOf(
        "¥\\s*([0-9]+\\.?[0-9]*)".toRegex(),
        "￥\\s*([0-9]+\\.?[0-9]*)".toRegex(),
        "([0-9]+\\.?[0-9]*)\\s*元".toRegex()
    )

    fun isPaymentNotification(packageName: String, title: String, text: String): Boolean {
        val combined = "$title $text"
        val hasAmount = amountPatterns.any { it.containsMatchIn(combined) }
        val hasPaymentKeyword = paymentKeywords.any { combined.contains(it) }
        return when (packageName) {
            wechatPackage, alipayPackage, pinduoduoPackage -> hasAmount && hasPaymentKeyword
            else -> false
        }
    }

    fun parsePayment(
        packageName: String,
        title: String,
        text: String,
        notificationId: String
    ): ExpenseRecord? {
        val combined = "$title $text"
        val amount = extractAmount(combined) ?: return null

        val platform = when (packageName) {
            wechatPackage -> "微信"
            alipayPackage -> "支付宝"
            pinduoduoPackage -> "拼多多"
            else -> return null
        }

        val merchant = extractMerchant(combined, title, platform)

        return ExpenseRecord(
            amount = amount,
            merchant = merchant,
            platform = platform,
            paymentChannel = parseChannel(combined, platform),
            category = "",
            recordedAt = System.currentTimeMillis(),
            notificationId = notificationId
        )
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in amountPatterns) {
            val match = pattern.find(text)
            val amount = match?.groupValues?.get(1)?.toDoubleOrNull()
            if (amount != null && amount > 0) return amount
        }
        return null
    }

    private fun extractMerchant(text: String, title: String, platform: String): String {
        val merchantPatterns = listOf(
            "向\\s*(.+?)\\s*(支付|付款|消费|缴费)".toRegex(),
            "(.+?)\\s*(收款|商户)".toRegex(),
            "在\\s*(.+?)\\s*(消费|支付|付款)".toRegex(),
            "(.+?)\\s*通过".toRegex()
        )

        for (pattern in merchantPatterns) {
            val match = pattern.find(text)
            val name = match?.groupValues?.get(1)?.trim()
            if (!name.isNullOrEmpty() && name.length < 30 && !name.contains("¥") && !name.contains("￥")) {
                return name
            }
        }

        if (title.isNotEmpty() && title.length < 20 &&
            !title.contains("支付") && !title.contains("付款") &&
            !title.contains("通知") && !title.contains("提醒")) {
            return title
        }

        return when (platform) {
            "拼多多" -> "拼多多"
            else -> "未知商户"
        }
    }

    private fun parseChannel(text: String, platform: String): String {
        return when {
            text.contains("零钱通") -> "零钱通"
            text.contains("零钱") -> "零钱"
            text.contains("余额宝") -> "余额宝"
            text.contains("余额") -> "余额"
            text.contains("花呗") -> "花呗"
            text.contains("信用卡") -> "信用卡"
            text.contains("借记卡") -> "借记卡"
            text.contains("储蓄卡") -> "储蓄卡"
            text.contains("银行卡") -> {
                if (platform == "拼多多" && text.contains("支付宝")) "支付宝调用银行卡"
                else "银行卡"
            }
            text.contains("支付宝") && platform == "拼多多" -> "支付宝"
            text.contains("微信") && platform == "拼多多" -> "微信"
            else -> "未知"
        }
    }
}