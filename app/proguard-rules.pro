# ==================== libbox (gomobile) ====================
# gomobile 生成的 Java 绑定通过反射调用 Go 方法，必须完整保留。
-keep class io.nekohasekai.libbox.** { *; }
-keep class go.** { *; }
-keep class go.Seq$Proxy { *; }
-dontwarn io.nekohasekai.libbox.**
-dontwarn go.**

# ==================== kotlinx.serialization ====================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class cn.moncn.sing_box_windows.** {
    *** Companion;
}
-keepclasseswithmembers class cn.moncn.sing_box_windows.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class cn.moncn.sing_box_windows.**$$serializer { *; }

# ==================== SnakeYAML ====================
# SnakeYAML 通过反射构造对象。
-keep class org.yaml.snakeyaml.** { *; }
-dontwarn org.yaml.snakeyaml.**

# ==================== OkHttp / Okio ====================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ==================== Compose ====================
-dontwarn androidx.compose.**
