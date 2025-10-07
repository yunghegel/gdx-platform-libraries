package dev.jamiecrown.gdx.ui.widget.console

import kotlin.reflect.KFunction

data class CLICommand(val obj: Any, val function: KFunction<*>, val description : String = generateHelp(function)) {
    companion object {
        fun generateHelp(function: KFunction<*>) : String {
            val command = function.annotations.filterIsInstance<Command>().firstOrNull()
            val args = function.parameters.joinToString(", ") { param ->
                val arg = param.annotations.filterIsInstance<Argument>().firstOrNull()
                val opt = param.annotations.filterIsInstance<Option>().firstOrNull()
                val flg = param.annotations.filterIsInstance<Flag>().firstOrNull()
                when {
                    arg != null -> "${arg.name}: ${param.type}"
                    opt != null -> "[${opt.name}=${opt.default}]: ${param.type}"
                    flg != null -> "[${flg.name}]: ${param.type}"
                    else -> "${param.name}: ${param.type}"
                }
            }
            return "${command?.name ?: function.name}($args): ${command?.description ?: "No description"}"
        }
    }
}