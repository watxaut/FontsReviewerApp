# ============================================================================
# FontsReviewer - Production ProGuard Rules
# ============================================================================

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================================
# Security: Strip Debug Logging in Production
# ============================================================================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Remove debug-only code
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

# ============================================================================
# Kotlin
# ============================================================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# ============================================================================
# Coroutines
# ============================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============================================================================
# Jetpack Compose
# ============================================================================
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.** {
    *;
}
-dontwarn androidx.compose.**

# Keep Compose @Composable functions
-keep @androidx.compose.runtime.Composable public class * {
    public <methods>;
}

# ============================================================================
# Hilt / Dagger
# ============================================================================
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.internal.Binding
-keep class * extends dagger.internal.ModuleAdapter
-keep class * extends dagger.internal.StaticInjection
-keepclassmembers class * {
    @javax.inject.* <fields>;
    @javax.inject.* <methods>;
    @dagger.* <fields>;
    @dagger.* <methods>;
}

# Keep Hilt generated classes
-keep class **_HiltModules** { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class **_Impl { *; }
-keep class * extends dagger.hilt.** { *; }

# ============================================================================
# Room Database
# ============================================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public abstract <methods>;
}
-keep @androidx.room.Dao interface *

# ============================================================================
# Supabase & Ktor
# ============================================================================
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep serialization classes
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializable classes
-keep,includedescriptorclasses class com.watxaut.fontsreviewer.**$$serializer { *; }
-keepclassmembers class com.watxaut.fontsreviewer.** {
    *** Companion;
}
-keepclasseswithmembers class com.watxaut.fontsreviewer.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all DTOs (data transfer objects)
-keep class com.watxaut.fontsreviewer.data.remote.dto.** { *; }

# Keep @Serializable classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep field names for kotlinx.serialization
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    <fields>;
}

# ============================================================================
# Mapbox
# ============================================================================
-keep class com.mapbox.** { *; }
-keep interface com.mapbox.** { *; }
-dontwarn com.mapbox.**

# Mapbox Native
-keep class com.mapbox.maps.** { *; }
-keep class com.mapbox.common.** { *; }
-dontwarn com.mapbox.maps.**

# ============================================================================
# Navigation Component
# ============================================================================
-keep class androidx.navigation.** { *; }
-keepnames class androidx.navigation.fragment.NavHostFragment

# ============================================================================
# Coil (Image Loading)
# ============================================================================
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================================================
# DataStore
# ============================================================================
-keep class androidx.datastore.*.** { *; }

# ============================================================================
# Domain Models (Keep for debugging crashes)
# ============================================================================
-keep class com.watxaut.fontsreviewer.domain.model.** { *; }

# ============================================================================
# Keep ViewModels
# ============================================================================
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# ============================================================================
# Gson (if used indirectly)
# ============================================================================
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================================================
# Remove Unused Code
# ============================================================================
-dontwarn org.slf4j.**
-dontwarn org.apache.commons.**
-dontwarn org.apache.http.**
-dontwarn javax.annotation.**

# ============================================================================
# Optimization Settings
# ============================================================================
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# Allow optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# ============================================================================
# Security: Obfuscate Package Names
# ============================================================================
-repackageclasses 'o'
-allowaccessmodification

# ============================================================================
# Native Methods (Keep for JNI if needed)
# ============================================================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================================================
# Parcelable
# ============================================================================
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ============================================================================
# Enums
# ============================================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================================
# Keep BuildConfig (for feature flags)
# ============================================================================
-keep class com.watxaut.fontsreviewer.BuildConfig { *; }

# ============================================================================
# Google Play Services Location
# ============================================================================
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**

# ============================================================================
# R8 Full Mode
# ============================================================================
-android
