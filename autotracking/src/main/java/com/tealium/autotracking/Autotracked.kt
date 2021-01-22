package com.tealium.autotracking

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
/**
 * Denotes that the annotated class is eligible for autotracking.
 * @param name  The name to use as the view name; defaults to the class simpleName
 * @param track Whether or not this class is eligible for autotracking. Defaults to `true` but can
 *              be used to disable auto tracking for specific tasks.
 */
annotation class Autotracked(
        val name: String = "",
        val track: Boolean = true
)