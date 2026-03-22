# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


# 1. Proteger tus modelos de datos (ajusta el paquete si es necesario)
# Esto evita que cambien los nombres de las variables que usas en JSON o Base de datos
-keep class com.jbncode.bibliaapp.domain.model.** { *; }
-keep class com.jbncode.bibliaapp.data.entities.** { *; }
-keep class com.jbncode.bibliaapp.data.networking.models.** { *; }

# 2. Reglas para evitar errores con Room y SQLite
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.RoomOpenHelper
-dontwarn androidx.room.**

# 3. Reglas para Dagger Hilt (ARREGLA EL CLASS CAST EXCEPTION)
-keep class com.jbncode.bibliaapp.viewmodel.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-dontwarn dagger.hilt.**
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# 4. Reglas para Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# 5. Reglas para Kotlinx Serialization (si la usas)
-keepclassmembers class com.jbncode.bibliaapp.** {
    *** Companion;
    *** $serializer;
}

# 6. Evitar que se eliminen métodos necesarios para AdMob
-keep class com.google.android.gms.ads.** { *; }

# ── Firebase ──────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ── WorkManager ───────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── Coroutines ────────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}