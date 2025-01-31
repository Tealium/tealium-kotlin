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
-dontwarn com.tealium.**
-dontshrink
-keepparameternames
-keeppackagenames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,
                SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class kotlin.Metadata { *; }

# Keep all public interfaces
-keep public interface com.tealium.** { *; }
-keep public class com.tealium.**$DefaultImpls { *; }
-keep public class AppCollectorConstants,
                    DeviceCollectorConstants,
                     TealiumCollectorConstants,
                     ConnectivityCollectorConstants,
                     TimeCollectorConstants,
                     CoreConstant,
                     DispatchType {
    public protected *;
}

# Keep all Collectors except internal ones
-keep public class !com.tealium.core.collection.Session**,
                    com.tealium.core.collection.* {
    public protected *;
}

# Keep all public Consent classes/enums, except internals.
-keep public class !com.tealium.core.consent.ConsentSharedPreferences,
                    com.tealium.core.consent.* {
    public protected *;
}

-keep public class com.tealium.core.events.EventTrigger$Companion {
    public protected *;
}

# Keep all public Messaging classes/enums, except internals.
-keep public class com.tealium.core.messaging.Messenger,
                    com.tealium.core.messaging.MessengerService {
    public protected *;
}
# Keep any Messenger subclasses
-keep public class com.tealium.** extends com.tealium.core.messaging.Messenger


-keep public class com.tealium.core.network.ConnectivityRetriever,
                    com.tealium.core.network.ConnectivityRetriever$Companion,
                    com.tealium.core.network.HttpClient,
                    com.tealium.core.network.ResourceRetriever,
                    com.tealium.core.network.ResourceEntity,
                    com.tealium.core.network.ResponseStatus,
                    com.tealium.core.network.ResponseStatus$*,
                    com.tealium.core.network.CooldownHelper,
                    com.tealium.core.network.CooldownHelper$CooldownStatus {
    public protected *;
}

# Keep expiry definitions.
-keep public class com.tealium.core.persistence.Expiry,com.tealium.core.persistence.Expiry$* {
    public protected *;
}

# Keep library settings data classes
-keep public class com.tealium.core.settings.LibrarySettings,
                    com.tealium.core.settings.Batching {
    public protected *;
}

# Public core classes - required by other modules.
-keep public class com.tealium.core.DateUtils,
                    com.tealium.core.DateUtils$Companion,
                    com.tealium.core.JsonLoader,
                    com.tealium.core.JsonLoader$Companion,
                    com.tealium.core.JsonUtils,
                    com.tealium.core.JsonUtils$Companion,
                    com.tealium.core.Logger,
                    com.tealium.core.Logger$Companion,
                    com.tealium.core.LogLevel$Companion,
                    com.tealium.core.ModuleManager,
                    com.tealium.core.Session,
                    com.tealium.core.Tealium,
                    com.tealium.core.Tealium$Companion,
                    com.tealium.core.TealiumConfig,
                    com.tealium.core.TealiumConfigKt,
                    com.tealium.core.TealiumContext,
                    com.tealium.core.TealiumEncoder,
                    com.tealium.core.TealiumEncoder$Companion,
                    com.tealium.core.DispatchType {
    public protected *;
}

# Keep public extension points
-keep public class com.tealium.core.Collectors,
                    com.tealium.core.Dispatchers,
                    com.tealium.core.Modules {
    public protected *;
}

-keep public enum com.tealium.** {
    public protected *;
}

# Keep public Dispatch implementatations
-keep class com.tealium.dispatcher.TealiumEvent,
            com.tealium.dispatcher.TealiumView,
            com.tealium.dispatcher.BatchDispatch,
            com.tealium.dispatcher.BatchDispatch$Companion,
            com.tealium.dispatcher.Dispatch$Keys
            { *; }

-keep class com.tealium.core.ActivityManager$* {
    *;
}
-keep class com.tealium.core.internal.TealiumInitProvider {
    public protected *;
}

# OpenForTesting Annotation
-keep class com.tealium.test.* { *; }