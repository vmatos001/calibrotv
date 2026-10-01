# ProGuard rules for CalibroTV

# Ignore missing annotation warnings from Tink / Security Crypto
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.crypto.tink.**
-dontwarn org.checkerframework.**

# Preserve Jetpack Compose & Material 3
-keep class androidx.compose.** { *; }
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }

# Preserve Data Models & Serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class com.example.calibretv.data.model.** { *; }
-keepclassmembers class com.example.calibretv.data.model.** { *; }

# Preserve Room Entities & Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.Dao *;
}

# Preserve EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# Keep Enum values & valueOf
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
