package com.example.autobookkeeper.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PaymentParser 核心识别逻辑单测
 * 覆盖: 黑名单 / 收入过滤 / 金额提取 / 包名识别
 */
class PaymentParserTest {

    private val parser = PaymentParser()

    @Test
    fun extractAmount_handles_yuan_prefix() {
        val amt = parser.extractAmountMultiPattern("已支付¥12.50")
        assertEquals(12.50, amt!!, 0.001)
    }

    @Test
    fun extractAmount_handles_yuan_suffix() {
        val amt = parser.extractAmountMultiPattern("消费 8.39 元")
        assertEquals(8.39, amt!!, 0.001)
    }

    @Test
    fun extractAmount_handles_comma_thousands() {
        val amt = parser.extractAmountMultiPattern("支付 ¥1,234.56")
        assertEquals(1234.56, amt!!, 0.001)
    }

    @Test
    fun extractAmount_returns_null_for_out_of_range() {
        val amt = parser.extractAmountMultiPattern("已支付¥0.00")
        assertNull(amt)
    }

    @Test
    fun isBlacklisted_blocks_summary_notification() {
        assertTrue(parser.isBlacklisted("微信支付", "本周消费统计 5 笔"))
    }

    @Test
    fun isBlacklisted_blocks_quota_reminder() {
        assertTrue(parser.isBlacklisted("微信支付", "可用额度提升至 50000"))
    }

    @Test
    fun isIncomeNotification_detects_received() {
        assertTrue(parser.isIncomeNotification("微信收款 100.00 元到账"))
    }

    @Test
    fun isPaymentNotification_blocks_non_whitelisted_package() {
        assertEquals(false, parser.isPaymentNotification("com.unknown.app", "支付", "已支付¥10"))
    }

    @Test
    fun isPaymentNotification_allows_wechat_paid() {
        assertTrue(
            parser.isPaymentNotification(
                "com.tencent.mm",
                "微信支付",
                "已支付¥8.39\n付款给：拼多多平台商户\n付款方式：零钱"
            )
        )
    }

    @Test
    fun isPaymentNotification_blocks_alipay_income_reminder() {
        assertEquals(
            false,
            parser.isPaymentNotification(
                "com.eg.android.AlipayGphone",
                "交易提醒",
                "收到转账 100.00 元"
            )
        )
    }

    @Test
    fun parsePayment_extracts_wechat_full_record() {
        val rec = parser.parsePayment(
            "com.tencent.mm",
            "微信支付",
            "已支付¥8.39\n付款给：拼多多平台商户\n付款方式：零钱",
            "nls_test"
        )
        assertNotNull(rec)
        assertEquals(8.39, rec!!.amount, 0.001)
        assertEquals("微信", rec.platform)
        assertEquals("零钱", rec.paymentChannel)
    }
}
