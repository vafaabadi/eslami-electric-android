# Conservative rules for a future minify-enabled release (v1 keeps minify off).
# Retrofit + kotlinx.serialization need keep rules before enabling isMinifyEnabled.

-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class retrofit2.Response

-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

-keep @kotlinx.serialization.Serializable class ** { *; }

-keep class com.eslamielectric.android.core.network.** { *; }
