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
# Hilt / Dagger 完整保留规则（必须完整）
# =============================================
-keep class dagger.hilt.** { *; }
-keep class dagger.assisted.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**
-dontwarn dagger.assisted.**

-keepclassmembers class * {
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <init>(...);
}

# Hilt 生成的内部类（工厂、组件、模块索引等）
-keep class *_HiltModules { *; }
-keep class *_HiltComponents { *; }
-keep class *$_Factory { *; }
-keep class *$_ProvideFactory { *; }

# Hilt Android Entry Point 生成代码
-keep class * extends dagger.hilt.android.internal.managers.ViewControllerComponentManager$FragmentContextWrapper { *; }

# Hilt ViewModel 工厂
-keepclasseswithmembers class * {
    <init>(**);
    @dagger.hilt.android.internal.lifecycle.HiltViewModelFactory$InjectedConstructor *;
}

# Module / InstallIn 注解元数据
-keep @interface dagger.Module
-keep @interface dagger.hilt.InstallIn
-keepclassmembers class * {
    @dagger.Module <fields>;
    @dagger.Module <methods>;
}

# =============================================
# Room 数据库运行时（增强版）
# =============================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.Dao { *; }
-dontwarn androidx.room.paging.**

# Room Entity 所有字段和注解必须原样保留（防止 R8 改字段名导致 SQLite 列不匹配）
-keepattributes Signature
-keepattributes *Annotation*
-keepnames class * implements java.io.Serializable
-keepnames class * implements android.os.Parcelable

# =============================================
# Retrofit + Gson 数据模型
# =============================================
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
# DataStore Preferences
# =============================================
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

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
