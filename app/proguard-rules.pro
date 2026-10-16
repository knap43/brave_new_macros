# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.knap43.macros.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Shizuku.newProcess is reached by reflection (see engine/Shell.kt); keep it and its process type.
-keep class rikka.shizuku.Shizuku { *; }
-keep class rikka.shizuku.ShizukuRemoteProcess { *; }
