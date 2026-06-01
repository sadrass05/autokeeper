package com.example.autobookkeeper.notification

import android.util.Log
import com.example.autobookkeeper.data.entity.ExpenseRecord
import javax.inject.Inject

class PaymentParser @Inject constructor() {

    private val wechatPackage = "com.tencent.mm"
    private val alipayPackage = "com.eg.android.AlipayGphone"
    private val pinduoduoPackage = "com.xunmeng.pinduoduo"
    private val unionPayPackage = "com.unionpay"
    private val meituanPackage = "com.sankuai.meituan"
    private val jdPackage = "com.jingdong.app.mall"

    private val cmbPackage = "cmb.pb"
    private val icbcPackage = "com.icbc"
    private val bocPackage = "com.chinamworld.boc"
    private val ccbPackage = "com.chinamworld.ccb"
    private val abcPackage = "com.android.bankabc"

    private val douyinPackage = "com.ss.android.ugc.aweme"
    private val kuaishouPackage = "com.smile.gifmaker"
    private val didiPackage = "com.sdu.didi.psnger"

    private val knownPackages = setOf(
        wechatPackage, alipayPackage, pinduoduoPackage, unionPayPackage, meituanPackage, jdPackage,
        cmbPackage, icbcPackage, bocPackage, ccbPackage, abcPackage,
        douyinPackage, kuaishouPackage, didiPackage
    )

    private val paymentKeywords = listOf(
        "支付", "付款", "消费", "扣费", "扣款", "交易", "买单", "缴费", "花费", "结算", "转账", "还款"
    )

    private val amountPatterns = listOf(
        "¥\\s*([0-9,]+\\.?[0-9]*)".toRegex(),
        "￥\\s*([0-9,]+\\.?[0-9]*)".toRegex(),
        "([0-9,]+\\.?[0-9]*)\\s*元".toRegex()
    )

    private val blacklistKeywords = listOf(
        "本周支付", "本月支付", "本周消费", "本月消费",
        "支付统计", "消费统计", "账单统计", "消费报告", "账单推送",
        "近7天", "近30天", "累计消费", "共消费",
        "月度账单", "年度账单", "账单查询",
        "额度提醒", "额度恢复", "可用额度",
        "限时优惠", "满减", "折扣", "优惠券",
        "积分兑换", "积分变动",
        "收款成功", "红包到账", "充值成功",
        "工资", "奖金", "报销", "理赔", "补偿金"
    )

    private val incomeKeywords = listOf(
        "到账", "收款", "转入", "退款", "退还", "入账",
        "红包", "返现", "补贴", "提现", "理赔", "补偿金",
        "工资到账", "奖金", "报销到账"
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
        val TAG = "PaymentParser"

        Log.d(TAG, "🔍 判断: pkg=$packageName, title=$title")

        if (isBlacklisted(title, text)) {
            Log.d(TAG, "❌ [黑名单] $title")
            return false
        }

        if (!knownPackages.contains(packageName)) {
            Log.d(TAG, "⏭ [跳过] 不支持的包名: $packageName")
            return false
        }

        if (packageName == wechatPackage) {
            val isWechatSummary = text.contains("笔交易") ||
                    text.contains("统计") ||
                    text.contains("共消费") ||
                    (title == "微信支付" && !text.contains("付款") && !text.contains("支付"))
            if (isWechatSummary) {
                Log.d(TAG, "❌ [微信汇总] $title")
                return false
            }
            val hasAmount = amountPatterns.any { it.containsMatchIn(combined) }
            if (!hasAmount) {
                Log.d(TAG, "❌ [微信] 无金额")
                return false
            }
            Log.d(TAG, "✅ [微信] 支付通知")
            return true
        }

        if (isIncomeNotification(combined)) {
            Log.d(TAG, "❌ [收入通知] $combined")
            return false
        }

        val hasAmount = amountPatterns.any { it.containsMatchIn(combined) }
        val hasPaymentKeyword = paymentKeywords.any { combined.contains(it) }

        Log.d(TAG, "📊 金额=$hasAmount, 关键词=$hasPaymentKeyword")

        if (!hasAmount) {
            Log.d(TAG, "❌ [${packageName}] 无金额")
            return false
        }

        return when (packageName) {
            alipayPackage -> {
                val hasAlipayContext = combined.contains("付款") ||
                    combined.contains("消费") || combined.contains("转账") ||
                    combined.contains("扣款") || combined.contains("还款") ||
                    combined.contains("缴费") || combined.contains("买单") ||
                    combined.contains("支付") || combined.contains("交易")
                if (!hasAlipayContext && !hasPaymentKeyword) {
                    Log.d(TAG, "❌ [支付宝] 有金额无支付上下文")
                    return false
                }
                Log.d(TAG, "✅ [支付宝] 支付通知")
                true
            }
            pinduoduoPackage, unionPayPackage, meituanPackage, jdPackage,
            cmbPackage, icbcPackage, bocPackage, ccbPackage, abcPackage,
            douyinPackage, kuaishouPackage, didiPackage -> {
                if (!hasPaymentKeyword) {
                    Log.d(TAG, "❌ [${packageName}] 无支付关键词")
                    return false
                }
                Log.d(TAG, "✅ [${packageName}] 支付通知")
                true
            }
            else -> {
                if (!hasPaymentKeyword) {
                    Log.d(TAG, "❌ [${packageName}] 无支付关键词")
                    return false
                }
                Log.d(TAG, "✅ [${packageName}] 支付通知(泛)")
                true
            }
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
            unionPayPackage -> "云闪付"
            meituanPackage -> "美团"
            jdPackage -> "京东"
            cmbPackage -> "招商银行"
            icbcPackage -> "工商银行"
            bocPackage -> "中国银行"
            ccbPackage -> "建设银行"
            abcPackage -> "农业银行"
            douyinPackage -> "抖音"
            kuaishouPackage -> "快手"
            didiPackage -> "滴滴"
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
            val raw = match?.groupValues?.get(1) ?: continue
            val cleaned = raw.replace(",", "")
            val amount = cleaned.toDoubleOrNull()
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
            "向\\s*(.+?)\\s*(支付|付款|消费|缴费|转账)".toRegex(),
            "(.+?)\\s*(收款|商户)".toRegex(),
            "在\\s*(.+?)\\s*(消费|支付|付款|转账)".toRegex(),
            "已向\\s*(.+?)\\s*(转账|付款|汇款)".toRegex(),
            "你已向\\s*(.+?)\\s*转".toRegex(),
            "信用卡(.+?)还款".toRegex(),
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
            "云闪付" -> "云闪付商户"
            "美团" -> "美团商户"
            "京东" -> "京东商户"
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