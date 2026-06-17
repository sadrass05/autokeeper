package com.example.autobookkeeper.notification

/**
 * NLS 健康状态机 - 纯函数, 无副作用, 便于单测
 *
 * 输入快照: notificationAccessGranted, nlsRunning, watchdogMissCount, restartGiveUp
 * 输出: Status 枚举
 *
 * 优先级 (从高到低):
 * - Revoked > GaveUp > Died > Healthy
 */
data class NlsHealthState private constructor(val status: Status) {
    enum class Status { Revoked, GaveUp, Died, Healthy }

    val shouldShowRevokedDialog: Boolean get() = status == Status.Revoked
    val shouldShowGaveUpDialog: Boolean get() = status == Status.GaveUp
    val shouldShowDiedDialog: Boolean get() = status == Status.Died
    val isHealthy: Boolean get() = status == Status.Healthy

    companion object {
        fun compute(
            notificationAccessGranted: Boolean,
            nlsRunning: Boolean,
            watchdogMissCount: Int,
            restartGiveUp: Boolean
        ): NlsHealthState {
            val status = when {
                !notificationAccessGranted -> Status.Revoked
                restartGiveUp -> Status.GaveUp
                !nlsRunning -> Status.Died
                else -> Status.Healthy
            }
            return NlsHealthState(status)
        }
    }
}
