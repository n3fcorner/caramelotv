# kotlinx.serialization gera serializers por reflexao sobre os @Serializable.
# Sem isto o R8 remove-os e o catalogo deixa de ler.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class pt.caramelo.tv.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class pt.caramelo.tv.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class pt.caramelo.tv.data.model.**$$serializer { *; }

# Media3 escolhe descodificadores por nome de classe.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
