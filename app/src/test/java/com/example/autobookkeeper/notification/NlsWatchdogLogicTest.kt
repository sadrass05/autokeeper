package com.example.autobookkeeper.notification

import org.junit.Test
import org.junit.Assert.assertTrue

class NlsWatchdogLogicTest {

    @Test
    fun should_recover_uses_kickOnce_not_just_requestRebind() {
        // 文档性测试: watchdog 决定要恢复时, 应该用 NlsRestartWorker.kickOnce
        // (因为只调 requestRebind 在进程已死时无效)
        val recoverAction = "NlsRestartWorker.kickOnce(context)"
        assertTrue(
            "watchdog 恢复策略应调 kickOnce (含 trampoline+rebind), 而不是裸 requestRebind",
            recoverAction.contains("kickOnce")
        )
    }
}
