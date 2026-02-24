# ProGuard rules for Resume Tailor

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.craftcv.app.data.models.** { *; }
-keepclassmembers class com.craftcv.app.data.models.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
