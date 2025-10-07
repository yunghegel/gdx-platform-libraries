package dev.jamiecrown.gdx.cli

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaType

/**
 * Central registry for CLI commands discovered from objects.
 * You can register manually or via Injector auto-detection (reflection hook).
 */
object CommandRegistry {
    private val commands: MutableMap<String, CommandEntry> = LinkedHashMap()

    data class CommandEntry(
        val key: String,
        val target: Any,
        val function: KFunction<*>,
        val meta: Command,
        val namespace: String?
    ) {
        val signature: String by lazy { buildSignature(function) }
        val help: String by lazy { buildHelp(function, meta, namespace) }
    }

    fun clear() { commands.clear() }

    /** Register all @Command methods on the instance. */
    fun register(instance: Any) {
        val k = instance::class
        val ns = k.findAnnotation<Namespace>()?.value
        for (m in k.declaredMemberFunctions) {
            val c = m.findAnnotation<Command>() ?: continue
            val key = buildKey(ns, c.name)
            commands[key] = CommandEntry(key, instance, m, c, ns)
        }
    }

    /** Remove all commands contributed by the instance. */
    fun unregister(instance: Any) {
        val it = commands.values.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (e.target === instance) it.remove()
        }
    }

    fun list(): List<CommandEntry> = commands.values.toList()

    fun get(name: String): CommandEntry? = commands[name]

    /** Execute a CLI text command. Returns either result or throws IllegalArgumentException on error. */
    fun execute(input: String): Any? {
        val tokens = CommandParser.tokenize(input)
        if (tokens.isEmpty()) throw IllegalArgumentException("Empty command")
        // Resolve command: try full "ns cmd" and then first token as command
        val (cmdKey, argsStartIdx) = resolveKey(tokens)
        val cmd = commands[cmdKey] ?: error("Unknown command '$cmdKey'. Known: ${commands.keys}")
        val remaining = tokens.drop(argsStartIdx)
        val callArgs = buildArguments(cmd.function, remaining)
        return cmd.function.call(cmd.target, *callArgs.toTypedArray())
    }

    private fun resolveKey(tokens: List<String>): Pair<String, Int> {
        if (tokens.size >= 2) {
            val combined = tokens[0] + " " + tokens[1]
            if (commands.containsKey(combined)) return combined to 2
        }
        val first = tokens[0]
        if (commands.containsKey(first)) return first to 1
        // Fallback: if there is exactly one command with that second token within any namespace
        if (tokens.size >= 2) {
            val second = tokens[1]
            val matches = commands.keys.filter { it.endsWith(" " + second) }
            if (matches.size == 1) return matches.first() to 2
        }
        // If first token equals a namespace and next token forms a valid command
        if (tokens.size >= 2) {
            val tries = commands.keys.filter { it.startsWith(tokens[0] + " ") && it.endsWith(" " + tokens[1]) }
            if (tries.isNotEmpty()) return (tokens[0] + " " + tokens[1]) to 2
        }
        throw IllegalArgumentException("Unknown command for tokens: ${tokens}. Known: ${commands.keys}")
    }

    private fun buildArguments(fn: KFunction<*>, tokens: List<String>): List<Any?> {
        val params = fn.parameters.filter { it.kind == KParameter.Kind.VALUE }
        val optionsSpec = params.mapNotNull { p -> p.findAnnotation<Option>()?.let { it.name to p } }.toMap()
        val paramSpec = params.filter { it.findAnnotation<Option>() == null }

        val (positional, options) = CommandParser.splitOptions(tokens)

        // Build positionals
        val argsMutable = ArrayList<Any?>(params.size)
        var posIdx = 0
        for (p in params) {
            val opt = p.findAnnotation<Option>()
            if (opt != null) {
                // Option parameter
                val present = options.containsKey(opt.name)
                val valueToken = options[opt.name]
                val value = if (isBoolean(p)) {
                    // boolean flag: presence toggles true unless explicit value provided
                    valueToken?.let { CommandParser.convert(it, p.type) } ?: (present)
                } else {
                    valueToken?.let { CommandParser.convert(it, p.type) }
                        ?: defaultOrThrow(p, opt.required, opt.default)
                }
                argsMutable += value
            } else {
                // Positional parameter
                val ann = p.findAnnotation<Parameter>()
                val token = positional.getOrNull(posIdx)
                val required = ann?.required ?: true
                val default = ann?.default ?: ""
                val value = token?.let { CommandParser.convert(it, p.type) }
                    ?: defaultOrThrow(p, required, default)
                argsMutable += value
                posIdx += 1
            }
        }

        // Validate extra positionals
        if (posIdx < positional.size) {
            throw IllegalArgumentException("Too many arguments. Got ${positional.size} but expected ${paramSpec.size}. Extra: ${positional.drop(posIdx)}")
        }

        return argsMutable
    }

    private fun defaultOrThrow(p: KParameter, required: Boolean, default: String): Any? {
        if (!required) {
            if (default.isNotEmpty()) return CommandParser.convert(default, p.type)
            // null permitted only if nullable
            return if (p.type.isMarkedNullable) null else CommandParser.zeroValue(p.type)
        }
        val ann = p.findAnnotation<Parameter>()
        val name = ann?.name ?: p.name
        throw IllegalArgumentException("Missing required parameter: ${name}")
    }

    private fun isBoolean(p: KParameter): Boolean {
        val jt = p.type.javaType
        return jt == java.lang.Boolean.TYPE || jt == java.lang.Boolean::class.java || p.type.toString() == "kotlin.Boolean"
    }

    private fun buildKey(ns: String?, cmd: String): String = if (ns.isNullOrBlank()) cmd else "$ns $cmd"

    private fun buildSignature(fn: KFunction<*>) : String {
        val parts = mutableListOf<String>()
        for (p in fn.parameters.filter { it.kind == KParameter.Kind.VALUE }) {
            val opt = p.findAnnotation<Option>()
            if (opt != null) {
                parts += "--${opt.name}"
            } else {
                val ann = p.findAnnotation<Parameter>()
                parts += (ann?.name ?: p.name ?: "arg")
            }
        }
        return parts.joinToString(" ")
    }

    private fun buildHelp(fn: KFunction<*>, meta: Command, ns: String?): String {
        val key = buildKey(ns, meta.name)
        val b = StringBuilder()
        b.append("$key - ${meta.description}\n")
        for (p in fn.parameters.filter { it.kind == KParameter.Kind.VALUE }) {
            val opt = p.findAnnotation<Option>()
            if (opt != null) {
                b.append("  --${opt.name}: ${opt.description}${if (opt.required) " (required)" else ""}\n")
            } else {
                val ann = p.findAnnotation<Parameter>()
                val name = ann?.name ?: p.name
                b.append("  ${name}: ${ann?.description ?: ""}${if (ann?.required == true) " (required)" else ""}\n")
            }
        }
        return b.toString().trimEnd()
    }

    // Reflection hook called from Injector without a compile-time dependency.
    @JvmStatic
    fun registerFromInjector(instance: Any) {
        try { register(instance) } catch (_: Throwable) { /* best-effort */ }
    }
}
