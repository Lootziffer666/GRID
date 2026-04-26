# Painkiller ProGuard / R8 rules.
# Keep kotlinx.serialization metadata for any data classes we serialize.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.painkiller.**$$serializer { *; }
-keepclassmembers class com.painkiller.** {
    *** Companion;
}
-keepclasseswithmembers class com.painkiller.** {
    kotlinx.serialization.KSerializer serializer(...);
}
