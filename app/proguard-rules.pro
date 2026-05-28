-keep class com.example.autobookkeeper.** { *; }
-keepclassmembers class com.example.autobookkeeper.** { *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * extends androidx.room.Entity {
    *;
}

-dontwarn org.apache.poi.**
-keep class org.apache.poi.** { *; }

-dontwarn com.google.mlkit.**
-keep class com.google.mlkit.** { *; }

-keep class org.openxmlformats.schemas.** { *; }
-dontwarn org.openxmlformats.schemas.**
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**

# =============================================
# Hilt 依赖注入（必须保留）
# =============================================
-keep class dagger.hilt.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <init>(...);
}
-dontwarn dagger.hilt.**

# =============================================
# Room 数据库运行时
# =============================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# =============================================
# Retrofit + Gson 数据模型
# =============================================
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# =============================================
# WorkManager 定时备份任务
# =============================================
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# =============================================
# MPAndroidChart 图表库
# =============================================
-keep class com.github.mikephil.charting.** { *; }

# =============================================
# Compose / Navigation / Material3（必须完整保留）
# =============================================
-keep class androidx.navigation.** { *; }
-keep class androidx.compose.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.animation.** { *; }
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.**
-dontwarn androidx.compose.material3.**

# =============================================
# OkHttp / Retrofit 内部类
# =============================================
-keepnames class okhttp3.**, okio.**
-dontwarn org.conscrypt.**
-dontwarn okhttp3.**

# =============================================
# Kotlin 协程
# =============================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# =============================================
# DataStore
# =============================================
-keep class androidx.datastore.** { *; }

# =============================================
# Release 包自动移除所有 Log 调用
# =============================================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# =============================================
# 移除 e.printStackTrace() 调用
# =============================================
-assumenosideeffects class java.lang.Exception {
    public void printStackTrace();
}
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}
