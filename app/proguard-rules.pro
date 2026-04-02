#############################################
# 🔐 GENERAL
#############################################

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

#############################################
# 🚫 IGNORE MISSING CLASSES (CRITICAL)
#############################################

-dontwarn android.os.ServiceManager
-dontwarn org.apache.batik.**
-dontwarn org.osgi.framework.**
-dontwarn org.apache.logging.log4j.**
-dontwarn javax.annotation.**
-dontwarn kotlin.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

#############################################
# 📦 GSON (MODELS)
#############################################

-keep class com.grf.model.** { *; }

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

#############################################
# 🌐 RETROFIT
#############################################

-keep interface com.grf.api.** { *; }

# Retrofit internal safety
-keep interface retrofit2.** { *; }
-keep class retrofit2.** { *; }

#############################################
# 🧬 ANDROIDX
#############################################

-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }

#############################################
# 📊 APACHE POI
#############################################

-keep class org.apache.poi.** { *; }

#############################################
# 📄 OPENCSV
#############################################

-keep class com.opencsv.** { *; }

#############################################
# 📦 LOCAL SDKs (VERY IMPORTANT 🔥)
#############################################

-keep class com.nlscan.** { *; }
-keep class com.rfid.** { *; }
-keep class com.device.** { *; }

#############################################
# 🧾 REMOVE LOGS (RELEASE CLEANUP)
#############################################

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

#############################################
# 🛡️ SAFETY FALLBACK (ONLY IF CRASHES)
#############################################

# Uncomment ONLY if something breaks
# -keep class com.grf.** { *; }