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

-keep class kotlin.Metadata { *; }

-keep interface com.tealium.visitorservice.VisitorUpdatedListener { *; }
-keep interface com.tealium.visitorservice.VisitorProfileManager { *; }

-keep class com.tealium.visitorservice.VisitorService { *; }
-keep class com.tealium.visitorservice.VisitorService$Companion { *; }
-keep class com.tealium.visitorservice.VisitorServiceKt { *; }
-keep class com.tealium.visitorservice.TealiumConfigVisitorServiceKt { *; }
-keep class com.tealium.visitorservice.CurrentVisit { *; }
-keep class com.tealium.visitorservice.VisitorProfile { *; }