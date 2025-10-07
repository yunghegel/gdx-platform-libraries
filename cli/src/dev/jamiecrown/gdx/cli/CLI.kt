package dev.jamiecrown.gdx.cli

/**
 * Annotation to describe a CLI command's paramter.
 *
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Parameter(val name: String, val description: String, val required: Boolean = true, val default: String = "")




/**
 * Annotation for a command to be run with refelction
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Command(val name: String, val description: String)


/**
 * Annotation to define a namespace for a set of commands,
 * i.e. $namespace $command / math add 1 2
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Namespace(val value: String)

/**
 * Option flag for a command, e.g. --verbose
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Option(val name: String, val description: String, val required: Boolean = false, val default: String = "")
