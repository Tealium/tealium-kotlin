package com.tealium.core

class LibrarySettingsExtractor {

    companion object {

        private const val REGEX_TIME_AMOUNT = "\\d+"
        private const val REGEX_TIME_UNIT = "[hmd]$"

        // Returns the time in seconds
        fun timeConverter(time: String): Int {
            val amount = Regex(REGEX_TIME_AMOUNT).find(time)?.let { amount ->
                val unit = Regex(REGEX_TIME_UNIT)
                unit.find(time)?.let {
                    amount.value.toInt() * toSeconds(it.value)
                }
            }
            return amount ?: -1
        }

        private fun toSeconds(unit: String): Int {
            return when (unit) {
                "d" -> 86400
                "h" -> 3600
                "m" -> 60
                else -> 60
            }
        }
    }
}