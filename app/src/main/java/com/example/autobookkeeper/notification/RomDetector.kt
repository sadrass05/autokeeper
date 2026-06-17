package com.example.autobookkeeper.notification

/**
 * 国产 ROM 检测 - 纯函数, 便于单测
 *
 * 通过读取 /system 的 ro.* 属性判断 (从 Build.PRODUCT 等常规字段无法区分 MIUI vs ColorOS)
 * App.onCreate() 时一次性读取并缓存到 App.currentRom
 */
data class Rom(val type: Type, val version: String) {
    enum class Type { Stock, MIUI, HyperOS, ColorOS, EMUI, OriginOS, OneUI, Unknown }

    val needsAutostartSetup: Boolean
        get() = type in setOf(Type.MIUI, Type.HyperOS, Type.ColorOS, Type.EMUI, Type.OriginOS)

    val needsBatteryOptimizationDisabled: Boolean
        get() = type in setOf(Type.MIUI, Type.HyperOS, Type.ColorOS, Type.EMUI, Type.OriginOS)

    val needsRecentsLock: Boolean
        get() = type in setOf(Type.MIUI, Type.HyperOS)

    /** 是否需要引导用户做 ROM 特定设置 */
    val needsRomSetup: Boolean
        get() = needsAutostartSetup
}

object RomDetector {
    /**
     * 检测当前 ROM 类型
     * @param props 模拟 Build.* / SystemProperties.get() 的 props, 便于测试
     */
    fun detect(props: Map<String, String>): Rom {
        val version = props["ro.build.version.release"] ?: "?"

        val type = when {
            props.containsKey("ro.miui.ui.version.name") -> Rom.Type.MIUI
            props.containsKey("ro.hyperos.version") -> Rom.Type.HyperOS
            props.containsKey("ro.oppo.theme.version") || props.containsKey("ro.coloros.version") -> Rom.Type.ColorOS
            props.containsKey("ro.build.version.emui") || props.containsKey("ro.build.hw_emui_api_level") -> Rom.Type.EMUI
            props.containsKey("ro.vivo.os.version") -> Rom.Type.OriginOS
            props.containsKey("ro.samsung.fingerprint") || props.containsKey("ro.samsung.build.fingerprint") -> Rom.Type.OneUI
            else -> Rom.Type.Stock
        }

        return Rom(type, version)
    }

    /**
     * 在运行时通过 SystemProperties 检测 (主进程)
     * Android 隐藏 API SystemProperties.get 通过反射调用
     */
    fun detectFromSystem(): Rom {
        val props = mutableMapOf<String, String>()
        val keys = listOf(
            "ro.miui.ui.version.name",
            "ro.hyperos.version",
            "ro.oppo.theme.version",
            "ro.coloros.version",
            "ro.build.version.emui",
            "ro.build.hw_emui_api_level",
            "ro.vivo.os.version",
            "ro.samsung.fingerprint",
            "ro.build.version.release"
        )
        for (key in keys) {
            try {
                val cls = Class.forName("android.os.SystemProperties")
                val method = cls.getMethod("get", String::class.java, String::class.java)
                val value = method.invoke(null, key, "") as String
                if (value.isNotEmpty()) props[key] = value
            } catch (e: Throwable) {
                // 忽略
            }
        }
        return detect(props)
    }
}
