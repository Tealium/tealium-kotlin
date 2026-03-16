package com.tealium.lifecycle

import java.util.Calendar

enum class FirstWakeType(val value: Int) {
    Month(3),
    Today(2),
    Neither(0);

    companion object {
        fun fromTimestamps(
            timestampA: Long,
            timestampB: Long,
            calendar: Calendar = Calendar.getInstance()
        ): FirstWakeType {
            calendar.timeInMillis = timestampA
            val monthA = calendar.get(Calendar.MONTH)
            val yearA = calendar.get(Calendar.YEAR)
            val dayA = calendar.get(Calendar.DAY_OF_YEAR)

            calendar.timeInMillis = timestampB
            val monthB = calendar.get(Calendar.MONTH)
            val yearB = calendar.get(Calendar.YEAR)
            val dayB = calendar.get(Calendar.DAY_OF_YEAR)

            var result = 0
            val isFirstWakeMonth = yearA != yearB || monthA != monthB

            if (isFirstWakeMonth) {
                result = Month.value
            }
            if (isFirstWakeMonth || dayA != dayB) {
                result = result or Today.value
            }

            return values().find { it.value == result } ?: Neither
        }
    }

    val isFirstWakeMonth: Boolean
        get() = value == Month.value

    val isFirstWakeToday: Boolean
        get() = value >= Today.value
}