package dev.jamiecrown.gdx.state.storage

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.Properties

/** Simple key/value store abstraction. */
interface KeyValueStore {
    fun write(name: String, values: Map<String, String>)
    fun read(name: String): Map<String, String>?
    fun exists(name: String): Boolean
}

/**
 * File-based implementation using .properties files per entry.
 * Files are stored at root/(namespace?)/name.properties
 *
 * Legacy support: reads old Java Properties-escaped files, but writes a
 * simple HOCON-like format to avoid ugly escaping.
 */
class FileKeyValueStore(
    private val root: File,
    private val namespace: String? = null
) : KeyValueStore {

    init {
        directory().mkdirs()
    }

    private fun directory(): File = if (namespace == null) root else File(root, namespace)

    private fun fileFor(name: String) = File(directory(), "$name.properties")

    override fun write(name: String, values: Map<String, String>) {
        val f = fileFor(name)
        f.parentFile.mkdirs()
        // Write in a simple HOCON-like format: key = value, comments with '#'
        FileOutputStream(f).use { out ->
            val charset = Charset.forName("UTF-8")
            val header = "# appstate: hocon-lite v1 for '$name'\n"
            out.write(header.toByteArray(charset))
            for ((k, v) in values) {
                // Do not escape: values are produced by StateWriter with its own safety
                // Keep keys as-is. Trim only trailing/leading whitespace in key for safety.
                val line = buildString {
                    append(k.trim())
                    append(" = ")
                    append(v)
                    append('\n')
                }
                out.write(line.toByteArray(charset))
            }
        }
    }

    override fun read(name: String): Map<String, String>? {
        val f = fileFor(name)
        if (!f.exists()) return null
        // Try to read our HOCON-like format first. If parsing yields nothing, fall back to Properties.
        val text = try { f.readText(Charset.forName("UTF-8")) } catch (_: Throwable) { null }
        if (text != null) {
            val parsed = parseHoconLike(text)
            if (parsed != null) return parsed
        }
        // Fallback: legacy Java Properties
        val props = Properties()
        FileInputStream(f).use { input -> props.load(input) }
        return props.entries.associate { (k, v) -> k.toString() to v.toString() }
    }

    private fun parseHoconLike(text: String): Map<String, String>? {
        val map = linkedMapOf<String, String>()
        var sawAnyLine = false
        val lines = text.lines()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) { continue }
            sawAnyLine = true
            if (line.startsWith("#") || line.startsWith("//")) continue
            // Find first = or : not inside quotes
            var idx = -1
            var inQuotes = false
            var escape = false
            for (i in line.indices) {
                val ch = line[i]
                if (escape) { escape = false; continue }
                when (ch) {
                    '\\' -> escape = true
                    '"' -> inQuotes = !inQuotes
                    '=', ':' -> if (!inQuotes) { idx = i; break }
                }
            }
            if (idx <= 0) continue
            val key = line.substring(0, idx).trim().trim('"')
            val value = line.substring(idx + 1).let {
                var s = it
                if (s.startsWith('=')) s = s.substring(1)
                s
            }.trim()
            map[key] = value
        }
        if (!sawAnyLine) return null
        return map
    }

    override fun exists(name: String): Boolean = fileFor(name).exists()
}
