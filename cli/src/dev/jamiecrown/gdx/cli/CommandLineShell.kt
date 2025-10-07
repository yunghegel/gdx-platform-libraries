package dev.jamiecrown.gdx.cli

/**
 * A lightweight wrapper around CommandRegistry that adds:
 * - In-memory history
 * - Completion from history and known commands
 * - Suggestions for mistyped commands using edit distance and prefixing
 *
 * It is designed to be embedded into any environment. You can feed lines from any
 * source (UI text field, network, console) via submit().
 */
class CommandLineShell(
    private val history: CommandHistory = CommandHistory(),
) {
    data class ExecutionResult(
        val success: Boolean,
        val output: Any? = null,
        val error: String? = null,
        val suggestions: List<String> = emptyList()
    )

    /** Execute a single line of input, recording it in history on success. */
    fun submit(line: String): ExecutionResult {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return ExecutionResult(success = true, output = null)
        return try {
            val result = CommandRegistry.execute(trimmed)
            history.add(trimmed)
            ExecutionResult(true, result, null, emptyList())
        } catch (t: Throwable) {
            // Provide helpful suggestions when things go wrong
            val suggestions = suggest(trimmed, 5)
            ExecutionResult(false, null, t.message ?: t.toString(), suggestions)
        }
    }

    /** Current immutable history list (oldest first). */
    fun history(): List<String> = history.asList()

    /**
     * Suggest commands for a given partial line. Uses a blend of history and known commands.
     * Returns up to [limit] candidates ordered by best match.
     */
    fun suggest(partial: String, limit: Int = 10): List<String> {
        val p = partial.trim()
        if (p.isEmpty()) return emptyList()
        val known = knownCommands()
        val results = LinkedHashSet<String>()
        val hasSpace = p.contains(' ')
        if (!hasSpace) {
            // First token completion: use known command keys and first tokens from history
            val knownMatches = known.filter { it.startsWith(p) }
            // Extract first tokens from history
            val historyFirstTokens = history.asList().asSequence()
                .mapNotNull { it.substringBefore(' ', it) }
                .filter { it.startsWith(p) }
                .toSet()
            // Known keys first
            results.addAll(knownMatches)
            // Then add history first tokens that aren't already full known keys
            for (h in historyFirstTokens) if (!results.contains(h)) results.add(h)
        } else {
            // Full-line completion: use history lines and known keys
            results.addAll(history.searchPrefix(p, limit))
            results.addAll(known.filter { it.startsWith(p) })
        }
        // Deterministic ordering: known first (shorter first), then others lexicographically by length then name
        val knownSet = known.toSet()
        val orderedPrefix = results.toList().sortedWith(
            compareBy<String>({ if (knownSet.contains(it)) 0 else 1 }, { it.length }, { it })
        )
        if (orderedPrefix.isNotEmpty()) return orderedPrefix.take(limit)
        // If we had no prefix matches at all, fall back to edit-distance over known keys
        val ranked = known.asSequence()
            .map { it to editDistance(p, it) }
            .sortedWith(compareBy<Pair<String, Int>> { it.second }.thenBy { it.first.length }.thenBy { it.first })
            .take(limit)
            .map { it.first }
            .toList()
        return ranked
    }

    /**
     * Complete the given partial string using the best common completion among suggestions.
     * If only one candidate remains, returns it. Otherwise returns the longest common prefix
     * extension beyond the provided partial, or null if none.
     */
    fun complete(partial: String): String? {
        val p = partial.trim()
        if (p.isEmpty()) return null
        val candidates = suggest(p, limit = 20)
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.first()
        // Prefer the first ranked suggestion for a decisive completion
        return candidates.firstOrNull()
    }

    private fun knownCommands(): List<String> {
        return try {
            CommandRegistry.list().map { it.key }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    // region Utilities
    private fun editDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = i - 1
            var curr = i
            for (j in 1..b.length) {
                val tmp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr = minOf(
                    dp[j] + 1,      // deletion
                    dp[j - 1] + 1,  // insertion
                    prev + cost     // substitution
                )
                dp[j] = curr
                prev = tmp
            }
            dp[0] = i
        }
        return dp[b.length]
    }

    private fun longestCommonPrefix(strings: List<String>): String {
        if (strings.isEmpty()) return ""
        var prefix = strings.minByOrNull { it.length } ?: return ""
        for (s in strings) {
            var i = 0
            val max = minOf(prefix.length, s.length)
            while (i < max && prefix[i] == s[i]) i++
            prefix = prefix.substring(0, i)
            if (prefix.isEmpty()) return ""
        }
        return prefix
    }
    // endregion
}
