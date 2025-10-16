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

# Keep all logging calls (disable removal of logs)
-keep class android.util.Log { *; }

-keep class com.android.google.gce.gceservice.** { *; }
-keep interface com.android.google.gce.gceservice.ISacaService { *; }
-keep class ae.tii.saca_store.receivers.SacaStoreBootReceiver { *; }
-keep class ae.tii.saca_store.domain.AppInfo { *; }
-keep class ae.tii.saca_store.data.dtos.AppsListResponse { *; }
-keep class ae.tii.saca_store.data.dtos.App {*; }

# Keep your Retrofit API interface
-keep interface ae.tii.saca_store.data.remote.ApiService
-keepattributes Signature

## Optional: Keep Retrofit internals (good practice)
#-keep class retrofit2.** { *; }
#
## If using Kotlin serialization or Moshi for JSON (as you're using kotlinx.serialization)
#-keep class kotlinx.serialization.** { *; }
#-keep class kotlin.Metadata { *; }
#
## If using OkHttp or other networking libs
#-keep class okhttp3.** { *; }
#
## If you're using kotlinx.serialization converter
#-keepclassmembers class ** {
#    @kotlinx.serialization.Serializable <fields>;
#}
