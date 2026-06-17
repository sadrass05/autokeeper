package com.example.autobookkeeper

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * 回归测试: 防止 `App.clearNlsRestartAttempts` 再次退化为 TODO 桩
 *
 * 背景: 之前 companion object 有个 `val clearNlsRestartAttempts: Any { get() { TODO() } }` 桩,
 *       getter 抛 NotImplementedError, 导致 HomeScreen 按钮闪退 + NlsRestartWorker 永远没被调度.
 *       这个测试验证 companion 函数能正常重置计数.
 */
class AppRestartStateTest {

    @After
    fun cleanup() {
        // 重置静态状态, 避免污染其他测试
        App.nlsRestartAttempts = 0
        App.nlsRestartGiveUp = false
    }

    @Test
    fun clearNlsRestartAttempts_resets_attempts_to_zero() {
        App.nlsRestartAttempts = 5
        App.clearNlsRestartAttempts()
        assertEquals(0, App.nlsRestartAttempts)
    }

    @Test
    fun clearNlsRestartAttempts_resets_giveUp_to_false() {
        App.nlsRestartGiveUp = true
        App.clearNlsRestartAttempts()
        assertFalse(App.nlsRestartGiveUp)
    }

    @Test
    fun clearNlsRestartAttempts_does_not_throw() {
        // 关键回归测试: 之前抛 NotImplementedError
        App.nlsRestartAttempts = 3
        App.nlsRestartGiveUp = true
        App.clearNlsRestartAttempts()  // 不应抛
        assertEquals(0, App.nlsRestartAttempts)
        assertFalse(App.nlsRestartGiveUp)
    }

    @Test
    fun clearNlsRestartAttempts_works_on_fresh_state() {
        // 边界: 初始状态调用也不能抛
        App.clearNlsRestartAttempts()
        assertEquals(0, App.nlsRestartAttempts)
        assertFalse(App.nlsRestartGiveUp)
    }
}
