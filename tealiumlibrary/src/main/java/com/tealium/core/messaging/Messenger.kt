package com.tealium.core.messaging

import com.tealium.core.ActivityManager
import com.tealium.core.validation.DispatchValidator
import java.util.*
import kotlin.reflect.KClass

abstract class Messenger<T : EventListener>(listener: KClass<T>) {

    val listenerClass: KClass<T> = listener

    abstract fun deliver(listener: T)
}

abstract class ExternalMessenger<T: ExternalListener>(listener: KClass<T>): Messenger<T>(listener)

class ValidationChangedMessenger(private val override: Class<out DispatchValidator>? = null): Messenger<ValidationChangedListener>(ValidationChangedListener::class) {

    override fun deliver(listener: ValidationChangedListener) {
        listener.onRevalidate(override)
    }
}

class ActivityStatusChangedMessenger(private val status: ActivityManager.ActivityStatus) : Messenger<ActivityObserverListener>(ActivityObserverListener::class) {
    override fun deliver(listener: ActivityObserverListener) {
        val (type, activity) = status
        when (type) {
            ActivityManager.ActivityLifecycleType.Paused -> listener.onActivityPaused(activity)
            ActivityManager.ActivityLifecycleType.Resumed -> listener.onActivityResumed(activity)
            ActivityManager.ActivityLifecycleType.Stopped -> listener.onActivityStopped(activity, activity.isChangingConfigurations)
            else -> { /* unused */ }
        }
    }
}