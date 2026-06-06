# ============================================================
# Coin Master — ProGuard / R8 rules
# Applied for release builds only (isMinifyEnabled = true)
# ============================================================

# ── Android / General ───────────────────────────────────────

# Keep all Activity, Fragment, Service, BroadcastReceiver, ContentProvider
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.fragment.app.Fragment

# Keep Parcelable CREATOR fields
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep View constructors (used by layouts via reflection)
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# Keep R class members (resource references)
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ── Kotlin ──────────────────────────────────────────────────

# Kotlin metadata (needed for reflection and serialization)
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin stdlib
-dontwarn kotlin.**
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ── Hilt (Dependency Injection) ─────────────────────────────

# Hilt-generated components and modules must not be renamed
-keep class dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-dontwarn dagger.hilt.**

# Hilt uses javax.inject annotations
-keep class javax.inject.** { *; }
-dontwarn javax.inject.**

# ── Room (Database) ─────────────────────────────────────────

# Keep all @Entity, @Dao, @Database, @TypeConverter classes and their members
# so Room's generated code can reflect on them at runtime.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.TypeConverters class * { *; }
-keep @androidx.room.TypeConverter class * { *; }

# Keep Room-generated implementation classes (named *_Impl by convention)
-keep class **.*_Impl { *; }
-keep class **.*_Impl$* { *; }

# Keep all fields of Room entities to prevent column name renaming
-keepclassmembers @androidx.room.Entity class * { *; }

# ── DataStore (Preferences) ─────────────────────────────────

-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ── MPAndroidChart ──────────────────────────────────────────

# Chart views, data sets, and renderers are accessed via reflection
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# ── Navigation Component ─────────────────────────────────────

-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ── Suppress common library warnings ────────────────────────

-dontwarn sun.misc.**
-dontwarn java.lang.invoke.**
