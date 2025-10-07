package dev.jamiecrown.gdx.ui.widget.console

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Namespace(val name: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Command(val name: String, val description: String = "")

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Argument(val name: String, val description: String = "")

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Option(val name: String, val default: String = "", val description: String = "")

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Flag(val name: String, val description: String = "",val key: String = "")

typealias Cmd = Command
typealias Arg = Argument
typealias Opt = Option
typealias Flg = Flag

typealias NS = Namespace