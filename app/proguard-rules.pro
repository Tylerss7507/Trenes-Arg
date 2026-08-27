# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Gson: mantiene los campos de nuestros modelos de datos (se deserializan por nombre)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.trenya.app.data.remote.** { <fields>; }
-keep class com.trenya.app.data.model.** { <fields>; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# WorkManager
-keep class androidx.work.impl.WorkDatabase { *; }
