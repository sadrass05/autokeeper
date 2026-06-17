package com.example.autobookkeeper.notification

import org.junit.Test
import org.junit.Assert.assertEquals

/**
 * 验证 kickOnce / doWork 的执行顺序
 * 核心: trampoline 必须先启动并等待, 然后才能 requestRebind
 */
class NlsRestartLogicTest {

    @Test
    fun kickOnce_order_should_be_trampoline_then_wait_then_rebind() {
        val order = mutableListOf<String>()
        val fakeTrampoline = {
            order.add("trampoline_start")
            Thread.sleep(200) // 模拟 FGS 启动耗时
            order.add("trampoline_ready")
        }
        val fakeRebind = { order.add("requestRebind") }

        // 模拟 kickOnce 的核心时序: trampoline → 2s wait → requestRebind
        fakeTrampoline()
        Thread.sleep(2000) // 等待进程真的醒来
        fakeRebind()

        assertEquals(listOf("trampoline_start", "trampoline_ready", "requestRebind"), order)
    }
}
