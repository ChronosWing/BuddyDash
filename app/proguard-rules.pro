# ── BuddyDash ProGuard / R8 rules ────────────────────────────────────────────

# Keep line numbers for meaningful crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── OkHttp ───────────────────────────────────────────────────────────────────
# OkHttp platform detection uses reflection for optional dependencies
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── Coil ─────────────────────────────────────────────────────────────────────
# Coil relies on OkHttp; its consumer rules handle most cases.
# Keep the public API entry points just in case.
-keep class coil.** { *; }

# ── DataStore / Protobuf ─────────────────────────────────────────────────────
# AndroidX DataStore Preferences uses protobuf-lite internally.
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ── Kotlin ───────────────────────────────────────────────────────────────────
# Keep Kotlin metadata for internal inline/reified functions
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ── Android / Compose ────────────────────────────────────────────────────────
# Compose compiler plugin handles most R8 integration.
# Keep Composable functions that may be referenced by navigation.
-keep @interface androidx.compose.runtime.Composable
