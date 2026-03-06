# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Room rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>(...);
}
-keep class * extends androidx.room.Entity
-keep class * implements androidx.room.Dao

# Google API Client rules
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keepnames class * implements java.io.Serializable

# Gson rules
-keep class com.google.gson.** { *; }
-keep class com.google.api.client.json.gson.** { *; }

# Glide rules
-keep public class * implements com.github.bumptech.glide.module.GlideModule
-keep public class * extends com.github.bumptech.glide.module.AppGlideModule
-keep public enum com.github.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Lottie rules
-keep class com.airbnb.lottie.** { *; }

# MPAndroidChart rules
-keep class com.github.mikephil.charting.** { *; }

# Models/Entities to prevent obfuscation of fields (important for JSON/Room mapping)
-keep class com.example.businessproplus.Order { *; }
-keep class com.example.businessproplus.Party { *; }
-keep class com.example.businessproplus.Item { *; }
-keep class com.example.businessproplus.MissedItem { *; }
-keep class com.example.businessproplus.User { *; }
-keep class com.example.businessproplus.Category { *; }
-keep class com.example.businessproplus.UserActivity { *; }
-keep class com.example.businessproplus.CalculationHistory { *; }

# Keep line numbers for better crash logs
-keepattributes SourceFile,LineNumberTable
