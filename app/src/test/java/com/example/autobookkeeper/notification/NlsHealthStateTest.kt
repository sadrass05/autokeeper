package com.example.autobookkeeper.notification

import com.example.autobookkeeper.App
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class NlsHealthStateTest {

    @After
    fun cleanup() {
        App.notificationListenerRunning = false
        App.nlsWatchdogMissCount = 0
        App.nlsRestartAttempts = 0
        App.nlsRestartGiveUp = false
    }

    @Test
    fun revoked_when_notification_access_not_granted() {
        val s = NlsHealthState.compute(
            notificationAccessGranted = false,
            nlsRunning = false,
            watchdogMissCount = 0,
            restartGiveUp = false
        )
        assertEquals(NlsHealthState.Status.Revoked, s.status)
    }

    @Test
    fun healthy_when_running_and_granted() {
        val s = NlsHealthState.compute(
            notificationAccessGranted = true,
            nlsRunning = true,
            watchdogMissCount = 0,
            restartGiveUp = false
        )
        assertEquals(NlsHealthState.Status.Healthy, s.status)
    }

    @Test
    fun died_when_granted_but_not_running() {
        val s = NlsHealthState.compute(
            notificationAccessGranted = true,
            nlsRunning = false,
            watchdogMissCount = 2,
            restartGiveUp = false
        )
        assertEquals(NlsHealthState.Status.Died, s.status)
    }

    @Test
    fun gaveup_takes_priority_over_died() {
        val s = NlsHealthState.compute(
            notificationAccessGranted = true,
            nlsRunning = false,
            watchdogMissCount = 5,
            restartGiveUp = true
        )
        assertEquals(NlsHealthState.Status.GaveUp, s.status)
    }
}
