package dev.jamiecrown.gdx.ui.widget.console

import dev.jamiecrown.gdx.core.blue
import dev.jamiecrown.gdx.core.cyan
import dev.jamiecrown.gdx.core.green
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

class CommandLineParser(val context : ConsoleContext) {



    fun joinQuotedArgs(args: Array<String>): Array<String> {
        val joinedArgs = mutableListOf<String>()
        var quoted = false
        var single = false
        var currentArg = ""
        for (arg in args) {
            if (arg.startsWith("\"") || arg.startsWith("'")) {
                quoted = true
                single = arg.startsWith("'")
                currentArg = arg.substring(1)
            } else if (arg.endsWith("\"") || arg.endsWith("'")) {
                quoted = false
                currentArg += if (single)" $arg".substringBeforeLast("'")  else " $arg".substringBeforeLast("\"")
                joinedArgs.add(currentArg)
            } else if (quoted) {
                currentArg += " $arg"
            } else {
                joinedArgs.add(arg)
            }
        }
        return joinedArgs.toTypedArray()
    }

    fun replaceEnvVars(args: Array<String>): Array<String> {
        return args.map { arg ->
            if (arg.startsWith("$")) {
                context.env[arg.substring(1)] ?: arg
            } else {
                arg
            }
        }.toTypedArray()
    }

    fun commandHelp(commandName: String) {
        for ((namespace, commands) in context.commands) {
            for ((name, command) in commands) {
                if (name == commandName) {
                    val map = HashMap<String, String>()
                    val func = command.function
                    val args = func.parameters.drop(1).map { param ->
                        when {
                            param.hasAnnotation<Argument>() -> {
                                val parameterAnnotation = param.findAnnotation<Argument>()!!
                                val name = parameterAnnotation.name
                                val type = param.type.classifier.toString().substringAfterLast(".").lowercase()
                                map[name] = parameterAnnotation.description
                                "$name: ${type.blue()}"
                            }

                            param.hasAnnotation<Option>() -> {
                                val optionAnnotation = param.findAnnotation<Option>()!!
                                val name = optionAnnotation.name
                                val type = param.type.classifier.toString().substringAfterLast(".").lowercase()
                                map[name] = optionAnnotation.description
                                "$name: ${type.blue()}"
                            }

                            param.hasAnnotation<Flag>() -> {
                                val flagAnnotation = param.findAnnotation<Flag>()!!
                                val name = flagAnnotation.name
                                map[name] = flagAnnotation.description
                                "[${name.cyan()}]"
                            }

                            else -> throw IllegalArgumentException("Unknown parameter type: ${param.name}")
                        }
                    }
                    println("${name.green()} (${args.joinToString(", ")}) - ${command.description}")
                    map.forEach { (key, value) ->
                        if (value.isNotEmpty()) {
                            println("\t${key.cyan()} - ${value}")
                        }
                    }
                    return
                }
            }
        }
    }

    fun namespaceHelp(namespace: String) {
        for ((ns, commands) in context.commands) {
            if (ns == namespace) {
                for ((name, command) in commands) {
                    commandHelp(name)
                }
            }
        }
    }



    fun parse(args: Array<String>) : Any? {
        if (args.isEmpty()) {
            throw IllegalArgumentException("No command provided")
        }
        var args = joinQuotedArgs(args)

        args.forEachIndexed { index, arg ->
            if (arg.startsWith("$")) {
                args[index] = context.env[arg.substring(1)] ?: arg
            }
        }

        if (args.contains ("&&")) {
            val index = args.indexOf("&&")
            val first = args.sliceArray(0 until index)
            val second = args.sliceArray(index + 1 until args.size)
            parse(first)
            parse(second)
            return null
        }


//        if not namespaced, insert global namespace
        if (args[0] !in context.commands.keys) {
            args = arrayOf("global") + args
        }

        val namespace: String = args[0]
        val commandName: String = args[1]

        if (args.contains("--help") || args.contains("-h")) {
            if (args.size == 2) {
                namespaceHelp(namespace)
            } else {
                commandHelp(commandName)
            }
            return null
        }

        val command = context. commands[namespace]?.get(commandName) ?: context.commands["global"]?.get(commandName)
        ?: throw IllegalArgumentException("Unknown command: $namespace $commandName")


        val arguments = mutableMapOf<String, String>()
        val options = mutableMapOf<String, String>()
        val flags = mutableSetOf<String>()



        for (arg in args.drop(2)) {
            when {
                arg.startsWith("--") -> {
                    val parts = arg.substring(2).split("=")
                    if (parts.size == 2) {
                        options[parts[0]] = parts[1]
                    } else {
                        flags.add(parts[0])
                    }
                }
                arg.startsWith("-") -> {
                    flags.add(arg.substring(1))
                }

                arg.contains("=") -> {
                    val parts = arg.split("=")
                    if (parts.size == 2) {
                        arguments[parts[0]] = parts[1]
                    }
                }
                else -> {
                    if (arguments.size < command.function.parameters.size - 1) {
                        arguments[command.function.parameters[arguments.size + 1].name!!] = arg
                    }
                }
            }
        }
        return invokeCommand(command, arguments, options, flags)
    }

    fun parse(input: String) {
        val args = input.split(" ").toTypedArray()
        try {
            parse(args)
        } catch (e: IllegalArgumentException) {
            println(e.message)
        }
    }

    private fun invokeCommand(
        command: CLICommand,
        arguments: Map<String, String>,
        options: Map<String, String>,
        flags: Set<String>
    ) : Any? {
        val (obj, func) = command
        val params = func.parameters.drop(1).map { param ->
            when {
                param.hasAnnotation<Argument>() -> {
                    val name = param.findAnnotation<Argument>()!!.name
                    val value = arguments[name] ?: throw IllegalArgumentException("Missing argument: $name")
                    convertValue(value, param.type.classifier as KClass<*>)
                }

                param.hasAnnotation<Option>() -> {
                    val annotation = param.findAnnotation<Option>()!!
                    val name = annotation.name
                    val shortcut = annotation.name[0].toString()
                    val value = options[name] ?: options[shortcut] ?: annotation.default
                    convertValue(value, param.type.classifier as KClass<*>)
                }

                param.hasAnnotation<Flag>() -> {
                    val annotation = param.findAnnotation<Flag>()!!
                    val name = annotation.name
                    val shortcut = annotation.name[0].toString()
                    flags.contains(annotation.name) || flags.contains(annotation.key) || flags.contains(shortcut)
                }

                else -> throw IllegalArgumentException("Unknown parameter type: ${param.name}")
            }
        }
        return func.call(obj, *params.toTypedArray())
    }



    private fun convertValue(value: String, targetType: KClass<*>): Any {
        return when (targetType) {
            String::class -> value
            Int::class -> value.toInt()
            Float::class -> value.toFloat()
            Boolean::class -> value.toBoolean()
            List::class -> {
                parseList(value, inferListTypeFromValue(value))
            }

            else -> throw IllegalArgumentException("Unsupported argument type: $targetType")
        }
    }

    fun inferListTypeFromValue(value: String): KClass<*> {
        val values = value.trim('(', ')').split(',').map { it.trim() }
        return when {
            values.all { it.toIntOrNull() != null } -> Int::class
            values.all { it.toFloatOrNull() != null } -> Float::class
            values.all { it.toBooleanStrictOrNull() != null } -> Boolean::class
            else -> String::class
        }
    }

    fun parseList(value: String, type: KClass<*>): List<*> {
        val values = value.trim('(', ')').split(',').map { it.trim() }
        return when (type) {
            Int::class -> values.map { it.toInt() }
            Float::class -> values.map { it.toFloat() }
            Boolean::class -> values.map { it.toBoolean() }
            else -> values
        }
    }

    @Command(name = "help", description = "print help")
    fun help() {
        for ((namespace, commands) in context.commands) {
            println("Namespace: $namespace")
            val info = HashMap<String, Pair<String, String>>()
            for ((name, command) in commands) {
                val func = command.function
                val args = func.parameters.drop(1).map { param ->
                    when {
                        param.hasAnnotation<Argument>() -> {
                            val parameterAnnotation = param.findAnnotation<Argument>()!!
                            info[name] = Pair(parameterAnnotation.name, parameterAnnotation.description)
                            val name = parameterAnnotation.name
                            val type = param.type.classifier.toString().substringAfterLast(".").lowercase()

                            "$name: $type"
                        }

                        param.hasAnnotation<Option>() -> {
                            val optionAnnotation = param.findAnnotation<Option>()!!
                            info[name] = Pair(optionAnnotation.name, optionAnnotation.description)
                            val name = optionAnnotation.name
                            val type = param.type.classifier.toString().substringAfterLast(".").lowercase()
                            "$name: $type"
                        }

                        param.hasAnnotation<Flag>() -> {
                            val flagAnnotation = param.findAnnotation<Flag>()!!
                            info[name] = Pair(flagAnnotation.name, flagAnnotation.description)
                            val name = flagAnnotation.name
                            "[$name]"
                        }

                        else -> throw IllegalArgumentException("Unknown parameter type: ${param.name}")
                    }
                }
                println("$name (${args.joinToString(", ")})")
                info[name]?.takeIf { it.second.isNotEmpty() }?.let { println("\t${it.first} - ${it.second}") }
            }
            println("--------------------")
        }
    }

    @Cmd("echo","echo message")
    fun echo(@Arg("message") message: String) {
        println(message)
    }

    @Cmd("set","set environment variable")
    fun set(@Arg("key") key: String, @Arg("value") value: String, @Flag("save") save: Boolean = false) {
        context.env[key] = value
        if (save) {
            saveEnv("env.txt")
        }
    }

    @Cmd("env","print environment variables")
    fun printenv() {
        for ((key, value) in context.env) {
            println("$key=$value")
        }
    }

    @Cmd("loadenv","load environment from file")
    fun loadEnv(@Arg("file") file: String) {
        val lines = java.io.File(file).readLines()
        for (line in lines) {
            val (key, value) = line.split("=", limit = 2)
            context.env[key] = value
        }
    }

    @Cmd("saveenv","save environment to file")
    fun saveEnv(@Arg("file", "filepath to save to") file: String) {
        java.io.File(file).writeText(context.env.map { (key, value) -> "$key=$value" }.joinToString("\n"))
    }

}