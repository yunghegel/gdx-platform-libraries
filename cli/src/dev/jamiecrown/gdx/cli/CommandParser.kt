package dev.jamiecrown.gdx.cli

import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.javaType

internal object CommandParser {
    fun tokenize(input: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        val n = input.length
        while (i < n) {
            while (i < n && input[i].isWhitespace()) i++
            if (i >= n) break
            val ch = input[i]
            when (ch) {
                '\'' , '"' -> {
                    val quote = ch
                    i++
                    val start = i
                    val sb = StringBuilder()
                    var escaped = false
                    while (i < n) {
                        val c = input[i]
                        if (escaped) {
                            sb.append(c)
                            escaped = false
                            i++
                            continue
                        }
                        if (c == '\\') { escaped = true; i++; continue }
                        if (c == quote) { i++; break }
                        sb.append(c)
                        i++
                    }
                    out += sb.toString()
                }
                else -> {
                    val sb = StringBuilder()
                    while (i < n && !input[i].isWhitespace()) {
                        sb.append(input[i])
                        i++
                    }
                    out += sb.toString()
                }
            }
        }
        return out.filter { it.isNotEmpty() }
    }

    /** Split tokens into positionals and option map. Options are --name value or --name=value */
    fun splitOptions(tokens: List<String>): Pair<List<String>, Map<String, String>> {
        val positionals = mutableListOf<String>()
        val options = linkedMapOf<String, String>()
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            if (t.startsWith("--")) {
                val eqIdx = t.indexOf('=')
                if (eqIdx > 2) {
                    val name = t.substring(2, eqIdx)
                    val value = t.substring(eqIdx + 1)
                    options[name] = value
                    i++
                } else {
                    val name = t.substring(2)
                    val value = tokens.getOrNull(i + 1)
                    if (value != null && !value.startsWith("--")) {
                        options[name] = value
                        i += 2
                    } else {
                        // boolean flag - presence only
                        options[name] = "true"
                        i += 1
                    }
                }
            } else {
                positionals += t
                i++
            }
        }
        return positionals to options
    }

    fun convert(token: String, type: KType): Any? {
        // List or array via [a,b,c]
        if (isList(type) || isArray(type)) {
            val items = parseList(token)
            val elemType = elementType(type)
            return items.map { convertScalar(it, elemType) }.let {
                if (isArray(type)) it.toTypedArray() else it
            }
        }
        return convertScalar(token, type)
    }

    private fun convertScalar(token: String, type: KType): Any? {
        val jt = type.javaType
        return when (jt) {
            String::class.java -> token
            java.lang.Integer.TYPE, Int::class.java -> token.toInt()
            java.lang.Long.TYPE, Long::class.java -> token.toLong()
            java.lang.Float.TYPE, Float::class.java -> token.toFloat()
            java.lang.Double.TYPE, Double::class.java -> token.toDouble()
            java.lang.Boolean.TYPE, Boolean::class.java -> token.toBooleanStrictOrNull() ?: (token != "false" && token != "0")
            else -> {
                // Enum?
                val jClass = jt as? Class<*>
                if (jClass != null && jClass.isEnum) {
                    @Suppress("UNCHECKED_CAST")
                    java.lang.Enum.valueOf(jClass as Class<out Enum<*>>, token)
                } else token
            }
        }
    }

    fun zeroValue(type: KType): Any? {
        val jt = type.javaType
        return when (jt) {
            java.lang.Integer.TYPE, Int::class.java -> 0
            java.lang.Long.TYPE, Long::class.java -> 0L
            java.lang.Float.TYPE, Float::class.java -> 0f
            java.lang.Double.TYPE, Double::class.java -> 0.0
            java.lang.Boolean.TYPE, Boolean::class.java -> false
            else -> null
        }
    }

    private fun parseList(token: String): List<String> {
        val t = token.trim()
        if (t.startsWith("[") && t.endsWith("]")) {
            val inner = t.substring(1, t.length - 1)
            return inner.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        }
        // Fallback: CSV without brackets
        return t.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun isArray(type: KType): Boolean {
        val jt = type.javaType as? Class<*> ?: return false
        return jt.isArray
    }

    private fun isList(type: KType): Boolean = type.toString().startsWith("kotlin.collections.List")

    private fun elementType(type: KType): KType {
        // crude but sufficient for our usage; we'll assume String elements if unknown
        val txt = type.toString()
        val start = txt.indexOf('<')
        val end = txt.lastIndexOf('>')
        return if (start > 0 && end > start) {
            val inner = txt.substring(start + 1, end)
            when (inner) {
                "kotlin.Int" -> Int::class.createType()
                "kotlin.Long" -> Long::class.createType()
                "kotlin.Float" -> Float::class.createType()
                "kotlin.Double" -> Double::class.createType()
                "kotlin.Boolean" -> Boolean::class.createType()
                else -> String::class.createType(nullable = false)
            }
        } else {
            String::class.createType()
        }
    }
}
