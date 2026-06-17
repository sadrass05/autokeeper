package com.example.autobookkeeper.notification

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * kickOnce 决策树单测
 * 核心: 进程状态决定执行路径
 *  - 进程已死 → trampoline + 长等待
 *  - 进程活着 → 直接 requestRebind, 短等待
 *  - NLS alive → 啥都不做
 */
class KickOnceStrategyTest {

    @Test
    fun process_alive_should_skip_trampoline() {
        val strategy = KickOnceStrategy.determine(processAlive = true, nlsAlive = false)
        assertEquals(false, strategy.shouldStartTrampoline)
        assertEquals(0L, strategy.waitBeforeRebindMs)
    }

    @Test
    fun process_dead_should_trampoline_with_long_wait() {
        val strategy = KickOnceStrategy.determine(processAlive = false, nlsAlive = false)
        assertEquals(true, strategy.shouldStartTrampoline)
        assertEquals(true, strategy.waitBeforeRebindMs >= 3000L)
    }

    @Test
    fun nls_alive_should_skip_rebind() {
        val strategy = KickOnceStrategy.determine(processAlive = true, nlsAlive = true)
        assertEquals(false, strategy.shouldStartTrampoline)
        assertEquals(false, strategy.shouldRequestRebind)
    }

    @Test
    fun process_dead_and_nls_alive_should_skip_rebind() {
        // 进程死但 nlsAlive=true (理论上不可能, 但决策树要处理)
        val strategy = KickOnceStrategy.determine(processAlive = false, nlsAlive = true)
        assertEquals(false, strategy.shouldRequestRebind)
    }
}
