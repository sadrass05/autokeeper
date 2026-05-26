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