package dev.jamiecrown.gdx.state.eventbus

/**
 * Annotation to mark a method to listen for a named event on the EventBus.
 * Methods may have zero parameters or a single reference-type parameter to receive event data.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Listen(val value: String)
