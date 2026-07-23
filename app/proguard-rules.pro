-keep class com.ambatrakan.sigma.data.model.** { *; }

# Keep Compose classes just in case
-keep class androidx.compose.** { *; }

# Keep navigation keys
-keep class com.ambatrakan.sigma.Tab { *; }
-keep class com.ambatrakan.sigma.Main { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
