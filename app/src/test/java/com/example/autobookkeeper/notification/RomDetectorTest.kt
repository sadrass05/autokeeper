package com.example.autobookkeeper.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class RomDetectorTest {

    @Test
    fun detect_xiaomi_via_ro_miui() {
        val props = mapOf(
            "ro.miui.ui.version.name" to "V14.0.5.0",
            "ro.build.version.release" to "13"
        )
        assertEquals(Rom.Type.MIUI, RomDetector.detect(props).type)
    }

    @Test
    fun detect_hyperos_via_ro_hyperos() {
        val props = mapOf(
            "ro.hyperos.version" to "1.0",
            "ro.build.version.release" to "14"
        )
        assertEquals(Rom.Type.HyperOS, RomDetector.detect(props).type)
    }

    @Test
    fun detect_oppo_via_ro_coloros() {
        val props = mapOf(
            "ro.oppo.theme.version" to "6.0",
            "ro.build.version.release" to "13"
        )
        assertEquals(Rom.Type.ColorOS, RomDetector.detect(props).type)
    }

    @Test
    fun detect_huawei_via_ro_emui() {
        val props = mapOf(
            "ro.build.version.emui" to "EmotionUI_12",
            "ro.build.version.release" to "12"
        )
        assertEquals(Rom.Type.EMUI, RomDetector.detect(props).type)
    }

    @Test
    fun detect_stock_android() {
        val props = mapOf(
            "ro.build.version.release" to "14"
        )
        assertEquals(Rom.Type.Stock, RomDetector.detect(props).type)
    }

    @Test
    fun needs_autostart_setup_for_miui() {
        val rom = Rom(type = Rom.Type.MIUI, version = "V14")
        assertEquals(true, rom.needsAutostartSetup)
    }

    @Test
    fun does_not_need_autostart_setup_for_stock() {
        val rom = Rom(type = Rom.Type.Stock, version = "14")
        assertEquals(false, rom.needsAutostartSetup)
    }
}
