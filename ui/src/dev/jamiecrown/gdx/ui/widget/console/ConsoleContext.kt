package dev.jamiecrown.gdx.ui.widget.console

import java.io.File
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

class ConsoleContext {

    internal val commands = mutableMapOf<String, MutableMap<String, CLICommand>>()
    internal val env = mutableMapOf<String, String>()
    internal val values = mutableMapOf<String, ArgType>()

    val namespaces : List<String>
        get() = (commands.keys.toList() + values.keys.toList()).distinct()

    fun registerCommands(vararg obj: Any) {
        for (o in obj) {

            val kClass = o::class
            val classNamespace = kClass.findAnnotation<Namespace>()?.name

            for (func in kClass.declaredFunctions) {
                val commandAnnotation = func.findAnnotation<Command>()

                val functionNamespace = func.findAnnotation<Namespace>()?.name
                val namespace = functionNamespace ?: classNamespace ?: "global"

                if (commandAnnotation != null) {
                    commands.computeIfAbsent(namespace) { mutableMapOf() }[commandAnnotation.name] =
                        CLICommand(o, func, commandAnnotation.description)
                }
            }
        }
    }


    @Cmd("set","set environment variable")
    fun set(@Arg("key") key: String, @Arg("value") value: String, @Flag("save") save: Boolean = false) {
        env[key] = value
        if (save) {
            saveEnv("env.txt")
        }
    }

    @Cmd("env","print environment variables")
    fun printenv() {
        for ((key, value) in env) {
            println("$key=$value")
        }
    }

    @Cmd("loadenv","load environment from file")
    fun loadEnv(@Arg("file") file: String) {
        val lines = File(file).readLines()
        for (line in lines) {
            val (key, value) = line.split("=", limit = 2)
            env[key] = value
        }
    }

    @Cmd("saveenv","save environment to file")
    fun saveEnv(@Arg("file", "filepath to save to") file: String) {
        File(file).writeText(env.map { (key, value) -> "$key=$value" }.joinToString("\n"))
    }

}