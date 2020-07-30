package com.tealium.core.messaging

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