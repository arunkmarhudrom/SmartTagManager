#############################################
# Production R8 / ProGuard rules
#############################################

# Keep useful metadata for runtime frameworks.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Source file names are obfuscated; keep line numbers for crash reporting.
-renamesourcefileattribute SourceFile
-keepattributes LineNumberTable

# Allow stronger optimization/obfuscation.
-allowaccessmodification
-repackageclasses

#############################################
# App entry points
#############################################

-keep class com.grf.smarttagmanager.MainActivity { *; }
-keep class com.grf.smarttagmanager.LoginActivity { *; }
-keep class com.grf.smarttagmanager.App { *; }

#############################################
# Retrofit / OkHttp
#############################################

-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

#############################################
# Gson model handling
#############################################

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep your API/model package names stable for JSON mapping safety.
-keep class com.grf.model.** { *; }
-keep interface com.grf.api.** { *; }

#############################################
# Third-party/local SDKs (RFID/scanner)
#############################################

-keep class com.nlscan.** { *; }
-dontwarn com.nlscan.**

-keep class com.rscja.** { *; }
-dontwarn com.rscja.**

-keep class com.rfid.** { *; }
-dontwarn com.rfid.**

-keep class com.device.** { *; }
-dontwarn com.device.**

# Zebra / API3 SDK (reflection-heavy; keep constructors and class names)
-keep class com.zebra.** { *; }
-dontwarn com.zebra.**

# Some transport classes are instantiated reflectively with no-arg constructors.
-keepnames class **Transport**
-keep class **Transport** { *; }
-keepclassmembers class **Transport** {
    public <init>();
}

#############################################
# Libraries used in app
#############################################

-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**

-dontwarn kotlin.**
-dontwarn javax.annotation.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.osgi.framework.**
-dontwarn org.apache.batik.**
-dontwarn android.os.ServiceManager
-dontwarn android.os.SystemProperties
-dontwarn java.awt.Shape
-dontwarn javax.xml.stream.**
-dontwarn net.sf.saxon.**

#############################################
# Remove noisy logs from release
#############################################

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
