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
-keepparameternames
-keeppackagenames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,
                SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontusemixedcaseclassnames

-keep class kotlin.Metadata { *; }

-keep interface com.tealium.momentsapi.** { *; }

-keep class com.tealium.momentsapi.MomentsApiService { *; }
-keep class com.tealium.momentsapi.MomentsApiServiceKt { *; }
-keep class com.tealium.momentsapi.EngineResponse { *; }
-keep class com.tealium.momentsapi.EngineResponse$Companion { *; }
-keep class com.tealium.momentsapi.MomentsApiRegion { *; }
-keep class com.tealium.momentsapi.MomentsApiRegion$** { *; }
-keep class com.tealium.momentsapi.ResponseListener { *; }
-keep class com.tealium.momentsapi.TealiumConfigMomentsApiKt { *; }
-keep class com.tealium.momentsapi.ErrorCode { *; }