package dev.jamiecrown.gdx.cli

/**
 * In-memory cyclic command history with simple prefix-based lookup.
 */
class CommandHistory(private val maxSize: Int = 200) {
    private val entries: ArrayDeque<String> = ArrayDeque()

    fun add(line: String) {

        val trimmed = line.trim()
        if (trimmed.isEmpty()) return
        // collapse duplicates if same as last
        if (entries.lastOrNull() == trimmed) return
        entries.addLast(trimmed)
        while (entries.size > maxSize) entries.removeFirst()
    }

    fun size(): Int = entries.size

    fun isEmpty(): Boolean = entries.isEmpty()

    fun asList(): List<String> = entries.toList()

    /** Most recent-first search of history commands starting with the given prefix. */
    fun searchPrefix(prefix: String, limit: Int = 10): List<String> {
        if (prefix.isEmpty()) return emptyList()
        val p = prefix.trim()
        if (p.isEmpty()) return emptyList()
        val out = ArrayList<String>(limit)
        for (i in entries.indices.reversed()) {
            val v = entries.elementAt(i)
            if (v.startsWith(p)) {
                out += v
                if (out.size >= limit) break
            }
        }
        return out
    }
}
