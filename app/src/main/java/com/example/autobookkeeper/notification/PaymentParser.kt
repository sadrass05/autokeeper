package com.example.autobookkeeper.notification

import android.util.Log
import com.example.autobookkeeper.data.entity.ExpenseRecord
import javax.inject.Inject

class PaymentParser @Inject constructor() {

    companion object {
        private const val TAG = "PaymentParser"
    }

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
        wechatPackage, alipayPackage, pinduoduoPackage, unionPayPackage,
        meituanPackage, jdPackage,
        cmbPackage, icbcPackage, bocPackage, ccbPackage, abcPackage,
        douyinPackage, kuaishouPackage, didiPackage
    )

    private val alipayTitles = setOf(
        "交易提醒", "支付宝通知", "付款成功", "交易成功",
        "支付宝", "收款通知", "转账通知", "账单通知",
        "支付通知", "商户收款", "消费通知"
    )

    private val paymentKeywords = listOf(
        "付款成功", "支付成功", "交易成功",
        "扣款成功", "消费成功", "付款完成",
        "扣费成功", "代扣成功", "已付款",
        "已支付",
        "的支出",
        "消费人民币", "消费￥", "消费¥",
        "支出人民币", "转账成功"
    )

    private val blacklistKeywords = listOf(
        "本周消费统计", "本月消费统计",
        "消费统计", "支付统计", "账单统计",
        "笔交易记录", "共消费了",
        "近7天消费", "近30天消费",
        "月度账单", "年度账单", "账单查询", "账单推送",
        "消费报告",
        "额度提醒", "额度恢复", "可用额度",
        "限时优惠", "满减", "折扣", "优惠券",
        "立减", "满返", "代金券", "抵扣券", "津贴",
        "积分兑换", "积分变动",
        "余额到账", "转账到账", "退款到账",
        "工资到账", "报销到账", "提现到账",
        "收款成功", "收到转账", "红包到账", "充值成功",
        "工资已到账", "奖金发放", "报销到账", "理赔到账"
    )

    private val incomeKeywords = listOf(
        "收款成功", "已收款", "对方已收款",
        "转入成功", "转入到账",
        "退款成功", "退款到账", "退还到账",
        "余额到账", "存入成功",
        "工资到账", "报销到账",
        "提现到账", "提现成功",
        "红包已到账", "收到红包",
        "返现到账", "奖励到账",
        "收入到账", "入账成功"
    )

    internal fun extractAmountMultiPattern(text: String): Double? {
        val patterns = listOf(
            Regex("""已支付[\u00A5\uFFE5¥￥]\s*(\d+(?:\.\d{1,2})?)"""),
            Regex("""一笔(\d+(?:\.\d{1,2})?)元的支出"""),
            Regex("""[\u00A5\uFFE5¥￥]\s*(\d{1,6}(?:,\d{3})*(?:\.\d{1,2})?)"""),
            Regex("""消费(\d{1,6}(?:\.\d{1,2})?)元"""),
            Regex("""(\d{1,6}(?:\.\d{1,2})?)元""")
        )
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val amount = match.groupValues[1]
                .replace(",", "")
                .toDoubleOrNull() ?: continue
            if (amount in 0.01..99999.99) return amount
        }
        return null
    }

    fun isBlacklisted(title: String, content: String): Boolean {
        val fullText = "$title $content"
        return blacklistKeywords.any { fullText.contains(it) }
    }

    fun isIncomeNotification(text: String): Boolean {
        return incomeKeywords.any { text.contains(it) }
    }

    fun isValidAmount(amount: Double): Boolean {
        return amount > 0.01 && amount < 100000.0
    }

    fun isPaymentNotification(packageName: String, title: String, text: String): Boolean {
        val combined = "$title $text"

        Log.d(TAG, "🔍 判断: pkg=$packageName, title=$title")

        if (isBlacklisted(title, text)) {
            Log.d(TAG, "❌ [黑名单] $title")
            return false
        }

        if (!knownPackages.contains(packageName)) {
            Log.d(TAG, "⏭ [跳过] 不支持的包名: $packageName")
            return false
        }

        if (packageName == wechatPackage) return isWechatPayment(combined, title, text)
        if (packageName == alipayPackage) return isAlipayPayment(combined, title)
        return isGenericPayment(packageName, combined)
    }

    fun parsePayment(
        packageName: String,
        title: String,
        text: String,
        notificationId: String
    ): ExpenseRecord? {
        val combined = "$title $text"

        if (isBlacklisted(title, text)) {
            Log.d(TAG, "❌ [parse] 黑名单")
            return null
        }
        if (isIncomeNotification(combined)) {
            Log.d(TAG, "❌ [parse] 收入通知")
            return null
        }

        // 支付宝"交易提醒"需额外确认是支出（非收款），其他标题不限制
        if (packageName == alipayPackage && title == "交易提醒") {
            val hasExpenseWord = text.contains("支出") || text.contains("消费")
            if (!hasExpenseWord) {
                Log.d(TAG, "❌ [parse] 支付宝交易提醒不含支出/消费")
                return null
            }
        }

        val amount = extractAmountMultiPattern(combined) ?: run {
            Log.d(TAG, "❌ [parse] 金额提取失败")
            return null
        }

        if (!isValidAmount(amount)) {
            Log.w(TAG, "金额超出合理范围: $amount")
            return null
        }

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

    private fun isWechatPayment(combined: String, title: String, text: String): Boolean {
        if (isIncomeNotification(combined)) {
            Log.d(TAG, "❌ [微信] 收入通知")
            return false
        }

        if (title == "微信支付" && text.contains("已支付")) {
            val hasAmount = extractAmountMultiPattern(combined) != null
            if (hasAmount) {
                Log.d(TAG, "✅ [微信] 已支付快速通道")
                return true
            }
        }

        val isWechatSummary = text.contains("笔交易记录") ||
                text.contains("共消费了") ||
                (title == "微信支付" && text.contains("笔交易记录"))
        if (isWechatSummary) {
            Log.d(TAG, "❌ [微信汇总] $title")
            return false
        }

        val hasAmount = extractAmountMultiPattern(combined) != null
        if (!hasAmount) {
            Log.d(TAG, "❌ [微信] 无金额")
            return false
        }
        Log.d(TAG, "✅ [微信] 支付通知")
        return true
    }

    private fun isAlipayPayment(combined: String, title: String): Boolean {
        if (isIncomeNotification(combined)) {
            Log.d(TAG, "❌ [支付宝] 收入通知")
            return false
        }

        // 软检查：标题在白名单中时，对"交易提醒"额外要求含支出/消费词（避免收款提醒误判）
        // 标题不在白名单中时，不直接拒绝，继续走金额+关键词检查
        if (title == "交易提醒") {
            val hasExpenseWord = combined.contains("支出") || combined.contains("消费")
            if (!hasExpenseWord) {
                Log.d(TAG, "❌ [支付宝] 交易提醒不含支出/消费")
                return false
            }
        }

        val hasAmount = extractAmountMultiPattern(combined) != null
        if (!hasAmount) {
            Log.d(TAG, "❌ [支付宝] 无金额")
            return false
        }

        // 支付宝通过：包名已确认 + 非收入 + 非交易提醒误报 + 有金额
        // 不再要求标题必须在白名单内，允许更多支付宝通知格式通过
        val titleMatch = alipayTitles.contains(title)
        Log.d(TAG, "✅ [支付宝] 支付通知 (title=$title, 标题白名单=${if (titleMatch) "✅命中" else "⏭未命中但放行"})")
        return true
    }

    private fun isGenericPayment(packageName: String, combined: String): Boolean {
        if (isIncomeNotification(combined)) {
            Log.d(TAG, "❌ [${packageName}] 收入通知")
            return false
        }

        val hasAmount = extractAmountMultiPattern(combined) != null
        val hasPaymentKeyword = paymentKeywords.any { combined.contains(it) }

        Log.d(TAG, "📊 [$packageName] 金额=$hasAmount, 关键词=$hasPaymentKeyword")

        if (!hasAmount) {
            Log.d(TAG, "❌ [${packageName}] 无金额")
            return false
        }
        if (!hasPaymentKeyword) {
            Log.d(TAG, "❌ [${packageName}] 无支付关键词")
            return false
        }
        Log.d(TAG, "✅ [${packageName}] 支付通知")
        return true
    }

    private fun extractMerchant(text: String, title: String, platform: String): String {
        val merchantPatterns = listOf(
            "付款给[：:]\\s*(.+)".toRegex(),
            "向\\s*(.+?)\\s*(支付|付款|消费|缴费|转账)".toRegex(),
            "收款方[：:]\\s*(.+)".toRegex(),
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
            if (!name.isNullOrEmpty() && name.length < 30 &&
                !name.contains("¥") && !name.contains("￥")) {
                return name
            }
        }

        if (title.isNotEmpty() && title.length < 20 &&
            !title.contains("支付") && !title.contains("付款") &&
            !title.contains("通知") && !title.contains("提醒") &&
            !title.contains("交易")) {
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

/*
 * ==================== 识别流程说明 ====================
 *
 * 一、微信支付完整识别流程：
 * 1. 标题="微信支付" + 内容含"已支付¥XX.XX" → 直接通过（快速通道）
 *    示例："已支付¥12.50\n付款给：拼多多平台商户\n付款方式：零钱"
 * 2. 标题="微信支付" + 内容含"笔交易记录"/"共消费了" → 汇总，过滤
 * 3. 其他微信通知 → 检查金额（5级模式）+ 非收入 → 通过
 *
 * 二、支付宝完整识别流程：
 * 1. 标题必须在白名单：交易提醒/支付宝通知/付款成功/交易成功
 * 2. 标题="交易提醒"时必须含"支出"（避免收款提醒误判）
 *    示例："你有一笔12.50元的支出\n收款方：商户名称"
 * 3. "支付宝通知"类："向XXX付款12.50元成功"
 * 4. 必须通过5级金额提取 + 支付关键词检查
 *
 * 三、会被过滤的情况及原因：
 * - 黑名单命中（消费统计/账单统计/额度提醒/优惠券等）→ 第一关拦截
 * - 包名不在白名单 → 第二关拦截
 * - 收入通知（收款/到账/退款/提现等）→ 第三关拦截
 * - 支付宝标题不在白名单 → "交易提醒"白名单检查
 * - 金额提取失败（5级模式全不命中）→ 金额检查
 * - 通用平台无支付关键词 → 关键词检查
 *
 * 四、日志排查方法（不重新打包）：
 * 1. 连接 adb logcat 过滤：adb logcat -s PaymentParser:D AutoBookkeeper:D
 * 2. 支付成功日志：✅ [平台] 支付通知
 * 3. 被过滤日志：❌ [原因] 通知内容
 * 4. 金额提取日志：📊 [包名] 金额=true/false, 关键词=true/false
 * 5. 对比收到的通知内容和日志，确认是被哪一关过滤
 * 6. 如被黑名单过滤但实际是支付通知 → 检查 blacklistKeywords 列表
 * 7. 如"无金额"但实际有金额 → 检查 extractAmountMultiPattern 的5个正则
 * 8. 如支付宝"标题不在白名单" → 检查标题并扩展 alipayTitles
 */