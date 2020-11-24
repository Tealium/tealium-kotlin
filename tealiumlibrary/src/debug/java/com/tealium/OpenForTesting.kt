package com.tealium.test

/**
 * Annotation to denote that a class should be made `open` for testing.
 * Available only in Debug builds to ensure release builds do not inadvertently leave a private
 * class open.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class OpenClass

/**
 * Annotation to denote that a class should be made `open` for testing.
 * This is non-functional in release builds.
 */
@OpenClass
@Target(AnnotationTarget.CLASS)
annotation class OpenForTesting