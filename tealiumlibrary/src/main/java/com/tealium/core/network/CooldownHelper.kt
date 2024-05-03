package com.tealium.core.network

import com.tealium.core.persistence.getTimestampMilliseconds
import kotlin.math.min

/**
 * A helper class to keep track of a cooldown period based on errors occurring.
 *
 * @param maxInterval The maximum amount of time a cooldown period can last.
 * @param baseInterval The amount of time to extend the cooldown for each consecutive failure, limited
 * by the [maxInterval]
 * @param timingProvider Provider function to provide the current time
 * @param initialStatus Defaults to [CooldownStatus.Success], but this is typically only used for testing.
 */
class CooldownHelper(
    val maxInterval: Long,
    val baseInterval: Long,
    private val timingProvider: () -> Long = ::getTimestampMilliseconds,
    initialStatus: CooldownStatus = CooldownStatus.Success,
) {
    enum class CooldownStatus {
        Success, Failure
    }

    private var status: CooldownStatus = initialStatus
    private var failureCount: Int = 0

    private val cooldownInterval: Long
        get() = min(maxInterval, baseInterval * failureCount)

    fun isInCooldown(lastFetch: Long) : Boolean {
        return if (status == CooldownStatus.Success) false else {
            lastFetch + cooldownInterval >= timingProvider.invoke()
        }
    }

    fun updateStatus(status: CooldownStatus) {
        if (status == CooldownStatus.Success) {
            failureCount = 0
        } else if (status == CooldownStatus.Failure) {
            failureCount++
        }

        this.status = status
    }

}