package com.tealium.test

/**
 * Annotation to denote that a class should be made `open` for testing.
 * This is non-functional in release builds.
 */
@Target(AnnotationTarget.CLASS)
annotation class OpenForTesting