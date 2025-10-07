package dev.jamiecrown.gdx.ui.widget.console

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

class CommandProcessor {

    private val commandObjects = mutableListOf<Any>()
    private val commands = mutableMapOf<String, CommandInfo>()

    data class CommandInfo(
        val function: KFunction<*>,
        val instance: Any,
        val description: String,
        val arguments: List<KParameter>
    )

    fun register(obj: Any) {
        commandObjects.add(obj)
        scanForCommands(obj)
    }

    private fun scanForCommands(obj: Any) {
        obj::class.declaredFunctions.forEach { function ->
            function.findAnnotation<Command>()?.let { commandAnnotation ->
                val commandName = commandAnnotation.name.lowercase()
                if (commands.containsKey(commandName)) {
                    println("Warning: Command '${commandName}' is already registered.")
                    return@forEach
                }

                val arguments = function.parameters.filter { it.kind == KParameter.Kind.VALUE }
                commands[commandName] = CommandInfo(
                    function,
                    obj,
                    commandAnnotation.description,
                    arguments
                )
            }
        }
    }

    fun execute(input: String): String {
        if (input.isBlank()) return ""

        val parts = input.trim().split(" ")
        val commandName = parts[0].lowercase()
        val args = parts.drop(1)

        val commandInfo = commands[commandName]
            ?: return "Error: Command not found: '$commandName'"

        if (args.size != commandInfo.arguments.size) {
            return "Error: Invalid number of arguments for '$commandName'. " +
                    "Expected ${commandInfo.arguments.size}, got ${args.size}."
        }

        return try {
            val convertedArgs = commandInfo.arguments.zip(args).map { (param, arg) ->
                convertArgument(arg, param.type.classifier as? kotlin.reflect.KClass<*>)
            }

            val allParams = listOf(commandInfo.instance) + convertedArgs
            commandInfo.function.call(*allParams.toTypedArray())?.toString() ?: "Command executed successfully."
        } catch (e: Exception) {
            "Error executing command '$commandName': ${e.message}"
        }
    }

    private fun convertArgument(arg: String, type: kotlin.reflect.KClass<*>?): Any {
        return when (type) {
            String::class -> arg
            Int::class -> arg.toInt()
            Float::class -> arg.toFloat()
            Boolean::class -> arg.toBoolean()
            else -> throw IllegalArgumentException("Unsupported argument type: $type")
        }
    }
}