# ==============================================================================
# Coffinity ProGuard Rules
# R8 minify + resource shrinking aktif
# ==============================================================================

# Generic Android
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes Exceptions, SourceFile, LineNumberTable

# Kotlin
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory {}

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase Auth
-keepattributes Signature
-keepattributes *Annotation*

# Firebase Firestore - data class'lar reflection ile serialize ediliyor
-keepclassmembers class com.icoffee.app.data.firebase.model.** {
    <init>();
    <fields>;
    <methods>;
}
-keepclassmembers class com.icoffee.app.data.model.** {
    <init>();
    <fields>;
    <methods>;
}

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }

# Gson - data class'lar
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp (Retrofit altyapı)
-dontwarn okhttp3.**
-dontwarn okio.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Google Maps
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.maps.**

# App-specific data classes - Firestore deserialization
-keep class com.icoffee.app.data.profile.** { *; }
-keep class com.icoffee.app.data.membership.** { *; }
-keep class com.icoffee.app.data.notifications.** { *; }
-keep class com.icoffee.app.notifications.** { *; }

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Crash diagnostics — stack trace okunabilir kalsın
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
