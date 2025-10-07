package dev.jamiecrown.gdx.state.app

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.Vector4
import dev.jamiecrown.gdx.state.storage.KeyValueStore
import java.util.ArrayDeque
import java.util.Locale

/** Writer for a state node: just a map of string->string. */
class StateWriter internal constructor() {
    private val data: MutableMap<String, String> = linkedMapOf()
    fun put(key: String, value: String) { data[key] = value }
    fun putInt(key: String, value: Int) = put(key, value.toString())
    fun putFloat(key: String, value: Float) = put(key, value.toString())
    fun putBoolean(key: String, value: Boolean) = put(key, value.toString())

    // Maps of primitive values
    fun putMapInt(key: String, map: Map<String, Int>) = put(key, formatTaggedMap("mapInt", map.mapValues { it.value.toString() }))
    fun putMapFloat(key: String, map: Map<String, Float>) = put(key, formatTaggedMap("mapFloat", map.mapValues { it.value.toString() }))
    fun putMapBoolean(key: String, map: Map<String, Boolean>) = put(key, formatTaggedMap("mapBool", map.mapValues { it.value.toString() }))
    fun putMapString(key: String, map: Map<String, String>) = put(key, formatTaggedMap("mapStr", map))

    // LibGDX ubiquitous types (YAML-like tagged inline lists)
    fun put(key: String, v: Vector2) = put(key, formatTaggedList("vec2", v.x, v.y))
    fun put(key: String, v: Vector3) = put(key, formatTaggedList("vec3", v.x, v.y, v.z))
    fun put(key: String, v: Vector4) = put(key, formatTaggedList("vec4", v.x, v.y, v.z, v.w))
    fun put(key: String, c: Color) = put(key, formatTaggedList("color", c.r, c.g, c.b, c.a))
    fun put(key: String, q: Quaternion) = put(key, formatTaggedList("quat", q.x, q.y, q.z, q.w))
    fun put(key: String, m: Matrix3) = put(key, formatTaggedList("mat3", *m.`val`))
    fun put(key: String, m: Matrix4) = put(key, formatTaggedList("mat4", *m.`val`))

    internal fun toMap(): Map<String, String> = data

    private fun formatTaggedList(tag: String, vararg f: Float): String {
        val joined = f.joinToString(", ") { it.toString() }
        return "!$tag [$joined]"
    }

    private fun formatTaggedMap(tag: String, map: Map<String, String>): String {
        val sb = StringBuilder()
        sb.append('!').append(tag).append(' ').append('{')
        var first = true
        for ((k, v) in map) {
            if (!first) sb.append(", ") else first = false
            sb.append(escapeKey(k)).append(": ")
            if (tag == "mapStr") {
                // Use single quotes to avoid escaping double quotes inside values
                sb.append('\'').append(escapeSingle(v)).append('\'')
            } else {
                sb.append(v)
            }
        }
        sb.append('}')
        return sb.toString()
    }

    private fun escapeKey(k: String): String {
        // Quote keys with single quotes if they contain spaces or separators, escaping single quotes by doubling
        return if (k.any { it.isWhitespace() || it in charArrayOf(':', ',', '{', '}', '"', '\'') }) {
            "'" + escapeSingle(k) + "'"
        } else k
    }

    private fun escapeSingle(s: String): String = buildString {
        for (ch in s) {
            when (ch) {
                '\'' -> append("''")
                else -> append(ch)
            }
        }
    }

    private fun escapeString(s: String): String = buildString {
        for (ch in s) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                else -> append(ch)
            }
        }
    }
}

/** Reader for a state node. */
class StateReader internal constructor(private val data: Map<String, String>) {
    fun get(key: String, default: String? = null): String? = data[key] ?: default
    fun getInt(key: String, default: Int = 0): Int = data[key]?.toIntOrNull() ?: default
    fun getFloat(key: String, default: Float = 0f): Float = data[key]?.toFloatOrNull() ?: default
    fun getBoolean(key: String, default: Boolean = false): Boolean = data[key]?.toBooleanStrictOrNull() ?: default

    // Maps of primitive values
    fun getMapInt(key: String, default: Map<String, Int> = emptyMap()): Map<String, Int> =
        parseMap(data[key])?.second?.mapValues { it.value.toIntOrNull() ?: 0 } ?: default

    fun getMapFloat(key: String, default: Map<String, Float> = emptyMap()): Map<String, Float> =
        parseMap(data[key])?.second?.mapValues { it.value.toFloatOrNull() ?: 0f } ?: default

    fun getMapBoolean(key: String, default: Map<String, Boolean> = emptyMap()): Map<String, Boolean> =
        parseMap(data[key])?.second?.mapValues { it.value.toBooleanStrictOrNull() ?: false } ?: default

    fun getMapString(key: String, default: Map<String, String> = emptyMap()): Map<String, String> =
        parseMap(data[key])?.second ?: default

    /** Returns the YAML-like type tag (e.g., "vec3", "quat") if present on the stored value. */
    fun getTypeTag(key: String): String? {
        val raw = data[key] ?: return null
        return parseListAndTag(raw)?.first ?: parseMap(raw)?.first
    }

    // LibGDX ubiquitous types
    fun getVector2(key: String, default: Vector2 = Vector2()): Vector2 =
        parseFloats(key)?.let { arr -> if (arr.size >= 2) Vector2(arr[0], arr[1]) else default } ?: default

    fun getVector3(key: String, default: Vector3 = Vector3()): Vector3 =
        parseFloats(key)?.let { arr -> if (arr.size >= 3) Vector3(arr[0], arr[1], arr[2]) else default } ?: default

    fun getVector4(key: String, default: Vector4 = Vector4()): Vector4 =
        parseFloats(key)?.let { arr -> if (arr.size >= 4) Vector4(arr[0], arr[1], arr[2], arr[3]) else default } ?: default

    fun getColor(key: String, default: Color = Color(1f, 1f, 1f, 1f)): Color =
        parseFloats(key)?.let { arr -> if (arr.size >= 4) Color(arr[0], arr[1], arr[2], arr[3]) else default } ?: default

    fun getQuaternion(key: String, default: Quaternion = Quaternion()): Quaternion =
        parseFloats(key)?.let { arr -> if (arr.size >= 4) Quaternion(arr[0], arr[1], arr[2], arr[3]) else default } ?: default

    fun getMatrix3(key: String, default: Matrix3 = Matrix3()): Matrix3 =
        parseFloats(key)?.let { arr -> if (arr.size >= 9) Matrix3(arr.copyOfRange(0, 9)) else default } ?: default

    fun getMatrix4(key: String, default: Matrix4 = Matrix4()): Matrix4 =
        parseFloats(key)?.let { arr -> if (arr.size >= 16) Matrix4(arr.copyOfRange(0, 16)) else default } ?: default

    private fun parseFloats(key: String): FloatArray? {
        val s = data[key] ?: return null
        val parsed = parseListAndTag(s) ?: return null
        return parsed.second
    }

    /**
     * Parses a value string in one of the following forms and returns (tag, floats):
     * - "!tag [a, b, c]" (preferred)
     * - "[a, b, c]"
     * - "a,b,c" (legacy)
     */
    private fun parseListAndTag(sRaw: String): Pair<String?, FloatArray>? {
        var s = sRaw.trim()
        if (s.isEmpty()) return null
        var tag: String? = null
        var listContent: String? = null

        if (s.startsWith("!")) {
            // find tag up to first whitespace or '['
            val afterBang = s.substring(1)
            var i = 0
            while (i < afterBang.length && !afterBang[i].isWhitespace() && afterBang[i] != '[') i++
            tag = afterBang.substring(0, i)
            val rest = afterBang.substring(i).trim()
            val lb = rest.indexOf('[')
            val rb = rest.lastIndexOf(']')
            if (lb >= 0 && rb > lb) {
                listContent = rest.substring(lb + 1, rb)
            } else {
                // No brackets after tag – try to parse legacy comma list after tag
                val valuesPart = rest.trim()
                listContent = valuesPart
            }
        } else if (s.startsWith("[")) {
            val lb = 0
            val rb = s.lastIndexOf(']')
            if (rb > lb) listContent = s.substring(lb + 1, rb) else return null
        } else {
            // Legacy plain comma-separated values
            listContent = s
        }

        val parts = listContent?.split(',')?.map { it.trim() } ?: return null
        val out = FloatArray(parts.size)
        for ((i, p) in parts.withIndex()) {
            val v = p.toFloatOrNull() ?: return null
            out[i] = v
        }
        return tag to out
    }

    /** Parses a value string of a tagged inline map and returns (tag, map<String,String>). */
    private fun parseMap(sRaw: String?): Pair<String?, Map<String, String>>? {
        if (sRaw == null) return null
        var s = sRaw.trim()
        if (s.isEmpty()) return null
        var tag: String? = null
        var content: String
        if (s.startsWith("!")) {
            val afterBang = s.substring(1)
            var i = 0
            while (i < afterBang.length && !afterBang[i].isWhitespace() && afterBang[i] != '{') i++
            tag = afterBang.substring(0, i)
            val rest = afterBang.substring(i).trim()
            val lb = rest.indexOf('{')
            val rb = rest.lastIndexOf('}')
            if (lb >= 0 && rb > lb) {
                content = rest.substring(lb + 1, rb)
            } else return null
        } else if (s.startsWith("{")) {
            val lb = 0
            val rb = s.lastIndexOf('}')
            if (rb > lb) content = s.substring(lb + 1, rb) else return null
        } else {
            // legacy plain k=v,k=v
            content = s
        }
        // Split entries by commas not in quotes (supports single or double quotes)
        val entries = mutableListOf<String>()
        run {
            val sb = StringBuilder()
            var inQuotes: Char? = null
            var esc = false
            for (ch in content) {
                if (esc) { sb.append(ch); esc = false; continue }
                when (ch) {
                    '\\' -> {
                        if (inQuotes == '"') { esc = true } else { sb.append('\\') }
                    }
                    '"', '\'' -> {
                        if (inQuotes == null) { inQuotes = ch; sb.append(ch) }
                        else if (inQuotes == ch) { inQuotes = null; sb.append(ch) }
                        else sb.append(ch)
                    }
                    ',' -> if (inQuotes == null) { entries += sb.toString(); sb.setLength(0) } else sb.append(ch)
                    else -> sb.append(ch)
                }
            }
            if (sb.isNotEmpty()) entries += sb.toString()
        }
        val map = linkedMapOf<String, String>()
        for (e in entries) {
            // Find first ':' or '=' not inside quotes
            var idx = -1
            var inQuotes: Char? = null
            var esc = false
            for (i in e.indices) {
                val ch = e[i]
                if (esc) { esc = false; continue }
                when (ch) {
                    '\\' -> esc = inQuotes == '"'
                    '"', '\'' -> inQuotes = if (inQuotes == null) ch else if (inQuotes == ch) null else inQuotes
                    ':', '=' -> if (inQuotes == null) { idx = i; break }
                }
            }
            if (idx <= 0) continue
            val kRaw = e.substring(0, idx).trim()
            val vRaw = e.substring(idx + 1).trim()
            val k = unquote(kRaw)
            val v = if ((vRaw.startsWith('"') && vRaw.endsWith('"') || vRaw.startsWith('\'') && vRaw.endsWith('\'')) && vRaw.length >= 2) unquote(vRaw) else vRaw
            map[k] = v
        }
        return tag to map
    }

    private fun unquote(s: String): String {
        var str = s.trim()
        if (str.length >= 2 && ((str.first() == '"' && str.last() == '"') || (str.first() == '\'' && str.last() == '\''))) {
            val quote = str.first()
            str = str.substring(1, str.length - 1)
            return if (quote == '\'') {
                // Single-quoted: interpret doubled single quotes as literal single quote, no backslash escapes
                buildString {
                    var i = 0
                    while (i < str.length) {
                        val ch = str[i]
                        if (ch == '\'' && i + 1 < str.length && str[i + 1] == '\'') { append('\''); i += 2 } else { append(ch); i++ }
                    }
                }
            } else {
                // Double-quoted: honor backslash escapes for \\ and \"
                val out = StringBuilder()
                var esc = false
                for (ch in str) {
                    if (esc) {
                        when (ch) {
                            '\\' -> out.append('\\')
                            '"' -> out.append('"')
                            else -> { out.append('\\'); out.append(ch) }
                        }
                        esc = false
                        continue
                    }
                    if (ch == '\\') { esc = true } else out.append(ch)
                }
                if (esc) out.append('\\') // trailing backslash literal
                out.toString()
            }
        }
        return str
    }
}

/**
 * A state node participating in app state persistence.
 * Provide a unique id, optional dependency ids, and read/write implementations.
 */
interface AppStateNode {
    val id: String
    val dependsOn: Set<String> get() = emptySet()
    /**
     * Default implementation uses reflection to persist fields annotated with @Persist.
     * Implementers can still override for custom behavior.
     */
    fun writeState(writer: StateWriter) {
        ReflectivePersistence.writeFields(this, writer)
    }
    /**
     * Default implementation uses reflection to restore fields annotated with @Persist.
     */
    fun readState(reader: StateReader) {
        ReflectivePersistence.readFields(this, reader)
    }
}

/** Manages registration, save, and load of state nodes in dependency order. */
class AppStateManager(private val store: KeyValueStore) {
    private val nodes: MutableMap<String, AppStateNode> = linkedMapOf()

    fun register(node: AppStateNode): AppStateManager {
        require(!nodes.containsKey(node.id)) { "Duplicate node id '${node.id}'" }
        nodes[node.id] = node
        return this
    }

    fun unregister(id: String) {
        nodes.remove(id)
    }

    fun saveAll() {
        for (id in topoOrder()) {
            val node = nodes[id] ?: continue
            val w = StateWriter()
            node.writeState(w)
            store.write(node.id, w.toMap())
        }
    }

    fun loadAll() {
        for (id in topoOrder()) {
            val node = nodes[id] ?: continue
            val map = store.read(node.id) ?: emptyMap()
            node.readState(StateReader(map))
        }
    }

    private fun topoOrder(): List<String> {
        // Kahn's algorithm
        val inDegree = mutableMapOf<String, Int>()
        val adj = mutableMapOf<String, MutableSet<String>>()
        for ((id, node) in nodes) {
            inDegree.putIfAbsent(id, 0)
            for (dep in node.dependsOn) {
                // if dep not registered, still create vertex to reflect external dep
                inDegree[id] = (inDegree[id] ?: 0) + 1
                adj.getOrPut(dep) { linkedSetOf() }.add(id)
            }
        }
        val q = ArrayDeque<String>()
        for ((id, deg) in inDegree) if (deg == 0) q.add(id)
        val order = mutableListOf<String>()
        while (q.isNotEmpty()) {
            val u = q.removeFirst()
            order += u
            for (v in adj[u].orEmpty()) {
                val d = (inDegree[v] ?: 0) - 1
                inDegree[v] = d
                if (d == 0) q.add(v)
            }
        }
        // If there is a cycle among registered nodes, place remaining nodes deterministically by id to avoid crash
        if (order.size != inDegree.size) {
            val remaining = inDegree.keys - order.toSet()
            order += remaining.sorted()
        }
        return order
    }
}
