# R8 rules for the release build.
#
# The app is pure Compose + kotlinx-free Kotlin: the AGP defaults already keep
# everything the code references directly. The rules below only protect the
# reflection-free surfaces R8 cannot see through resource lookups.

# Resource-backed entries resolved by name at runtime (drawable/string maps).
-keepclassmembers class com.aela.mainboardoverride.** {
    public static <fields>;
}

# Kotlin metadata keeps readable stack traces in crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
