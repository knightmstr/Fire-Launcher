# ProGuard / R8 rules for FireLauncher
-keep class com.knightmstr.firelauncher.** { *; }
-dontwarn androidx.tv.**
-dontwarn kotlinx.coroutines.**

# Preserve Kotlin metadata & annotations for Compose / DataStore
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
}

# Coil image loader compatibility
-keep class coil.** { *; }
-dontwarn coil.**
