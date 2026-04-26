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

-keep,includedescriptorclasses class com.painkiller.app.**$$serializer { *; }
-keepclassmembers class com.painkiller.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.painkiller.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
