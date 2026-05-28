package com.example.autobookkeeper.notification

import android.util.Log
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

    // ========== 黑名单关键词 ==========
    // 包含以下词的通知直接忽略（统计/广告/收入类）
    private val blacklistKeywords = listOf(
        // 统计类（非真实交易）
        "本周支付", "本月支付", "本周消费", "本月消费",
        "支付统计", "消费统计", "账单统计", "消费报告",
        "近7天", "近30天", "累计消费", "共消费",

        // 广告推送类
        "限时优惠", "满减", "折扣", "红包", "优惠券",
        "省了", "立减", "特惠", "活动", "推荐",

        // 收入类（不应记录为支出）
        "到账", "收款成功", "转入", "退款", "退钱",
        "红包到账", "收到", "入账", "充值成功",
        "工资", "奖金", "报销"
    )

    // ========== 收入判断白名单 ==========
    // 包含以下词时判定为收入消息，跳过记录
    private val incomeKeywords = listOf(
        "到账", "收款", "转入", "退款", "退还", "入账"
    )

    /**
     * 黑名单过滤：标题或内容包含黑名单关键词则返回 true
     *
     * 作用：在最早阶段过滤掉明显不是支出的通知
     * - 微信"本周消费¥5000"等统计汇总
     * - 淘宝/京东的促销广告
     * - 支付宝/微信的转账到账通知
     */
    fun isBlacklisted(title: String, content: String): Boolean {
        val fullText = "$title $content"
        return blacklistKeywords.any { keyword ->
            fullText.contains(keyword)
        }
    }

    /**
     * 收入判断：包含收入关键词则返回 true
     *
     * 作用：识别收入类消息，避免将收入误记为支出
     * 典型场景：
     * - "您已收到XXX转账100.00元"
     * - "退款已到账，金额50.00元"
     */
    fun isIncomeNotification(text: String): Boolean {
        return incomeKeywords.any { text.contains(it) }
    }

    /**
     * 金额有效性验证：过滤不合理金额
     *
     * 作用：排除解析错误导致的异常金额
     * - 最小值 0.01 元：过滤零金额和极小误差值
     * - 最大值 100,000 元：过滤系统错误或测试数据产生的大额
     */
    fun isValidAmount(amount: Double): Boolean {
        return amount > 0.01 && amount < 100000.0
    }

    /**
     * 判断是否为有效的支付通知
     *
     * 过滤优先级（从高到低）：
     * 1. 黑名单过滤 → 统计/广告/收入类直接忽略
     * 2. 收入过滤 → 到账/退款等跳过
     * 3. 微信特殊处理 → 过滤统计/汇总通知
     * 4. 基本条件检查 → 必须同时有金额和支付关键词
     */
    fun isPaymentNotification(packageName: String, title: String, text: String): Boolean {
        val combined = "$title $text"

        // 第一步：黑名单过滤（最高优先级）
        if (isBlacklisted(title, text)) {
            Log.d("PaymentParser", "被黑名单过滤: title=$title, content=$text")
            return false
        }

        // 第二步：收入过滤
        if (isIncomeNotification(combined)) {
            Log.d("PaymentParser", "识别为收入通知，跳过: $combined")
            return false
        }

        // 第三步：微信特殊处理
        // 微信的通知格式特殊，有很多非交易类的"微信支付"标题通知
        if (packageName == wechatPackage) {
            // 过滤微信统计/汇总类通知
            val isWechatSummary = text.contains("笔交易") ||
                    text.contains("统计") ||
                    text.contains("共消费") ||
                    (title == "微信支付" && !text.contains("付款") && !text.contains("支付"))
            if (isWechatSummary) {
                Log.d("PaymentParser", "微信汇总通知被过滤: title=$title")
                return false
            }
        }

        // 第四步：基本条件检查（必须有金额 + 支付关键词）
        val hasAmount = amountPatterns.any { it.containsMatchIn(combined) }
        val hasPaymentKeyword = paymentKeywords.any { combined.contains(it) }

        return when (packageName) {
            wechatPackage, alipayPackage, pinduoduoPackage -> hasAmount && hasPaymentKeyword
            else -> false
        }
    }

    /**
     * 解析支付通知并生成 ExpenseRecord
     *
     * 解析流程：
     * 1. 前置安全检查（黑名单 + 收入 + 金额有效性）
     * 2. 确定平台来源
     * 3. 微信额外验证（必须有商户信息）
     * 4. 提取商户名称和支付渠道
     * 5. 构建 ExpenseRecord 对象
     */
    fun parsePayment(
        packageName: String,
        title: String,
        text: String,
        notificationId: String
    ): ExpenseRecord? {
        val combined = "$title $text"

        // 前置过滤：再次确认不是黑名单/收入类
        if (isBlacklisted(title, text)) return null
        if (isIncomeNotification(combined)) return null

        // 提取金额
        val amount = extractAmount(combined) ?: return null

        // 金额范围验证
        if (!isValidAmount(amount)) {
            Log.w("PaymentParser", "金额超出合理范围: $amount")
            return null
        }

        // 确定平台
        val platform = when (packageName) {
            wechatPackage -> "微信"
            alipayPackage -> "支付宝"
            pinduoduoPackage -> "拼多多"
            else -> return null
        }

        // 微信额外验证：确保能提取到商户信息
        // 微信很多通知只有金额没有商户名，这类通常是汇总或广告
        if (packageName == wechatPackage) {
            val merchant = extractMerchant(combined, title, platform)
            if (merchant == "未知商户") {
                Log.d("PaymentParser", "微信通知无法提取商户信息，跳过")
                return null
            }
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

    /**
     * 提取商户名称
     *
     * 匹配策略（按优先级）：
     * 1. 正则匹配 "向XXX支付/付款" 格式
     * 2. 匹配 "XXX收款/商户" 格式
     * 3. 匹配 "在XXX消费/支付" 格式
     * 4. 使用通知标题作为商户名（需满足条件）
     * 5. 返回默认值 "未知商户"
     */
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

        // 标题作为备选商户名（排除通用词汇）
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

    /**
     * 解析支付渠道
     *
     * 根据通知内容中的关键词判断使用的支付方式
     * 如未匹配到任何渠道，返回默认值 "未知"
     */
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