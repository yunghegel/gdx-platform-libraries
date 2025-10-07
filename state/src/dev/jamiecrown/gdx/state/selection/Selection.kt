package dev.jamiecrown.gdx.state.selection

/** Policy governing behavior of a SelectionModel. */
data class SelectionPolicy<T>(
    val allowMultiple: Boolean = true,
    val allowEmpty: Boolean = true,
    val maxSelections: Int = Int.MAX_VALUE,
    val canSelect: (T) -> Boolean = { true },
    val equality: (T, T) -> Boolean = { a, b -> a == b }
)

/** Event describing a selection change. */
data class SelectionChangeEvent<T>(
    val before: List<T>,
    val added: List<T>,
    val removed: List<T>,
    val after: List<T>,
    val cause: String
)

fun interface SelectionListener<T> {
    fun onSelectionChanged(event: SelectionChangeEvent<T>)
}

/**
 * State-machine-esque selection container with observability and flexible policy.
 */
class SelectionModel<T>(
    private var policy: SelectionPolicy<T> = SelectionPolicy()
) {
    private val items = mutableListOf<T>() // preserves order of selection
    private val listeners = mutableListOf<SelectionListener<T>>()

    fun setPolicy(newPolicy: SelectionPolicy<T>) {
        policy = newPolicy
        // Reconcile current selection with new policy
        val before = items.toList()
        val filtered = before.filter(policy.canSelect)
        val truncated = if (!policy.allowMultiple && filtered.size > 1) listOf(filtered.first()) else filtered
        val capped = truncated.take(policy.maxSelections)
        val after = if (capped.isEmpty() && !policy.allowEmpty && filtered.isNotEmpty()) listOf(filtered.first()) else capped
        val removed = before.filter { b -> after.none { a -> policy.equality(a, b) } }
        if (removed.isNotEmpty()) {
            items.clear(); items.addAll(after)
            notifyListeners(before, added = emptyList(), removed = removed, cause = "policyChanged")
        }
    }

    fun addListener(l: SelectionListener<T>) { listeners += l }
    fun removeListener(l: SelectionListener<T>) { listeners -= l }

    fun getSelection(): List<T> = items.toList()

    fun clear(cause: String = "clear") = setAll(emptyList(), cause)

    fun setAll(newItems: Collection<T>, cause: String = "setAll") {
        val before = items.toList()
        val filtered = newItems.filter(policy.canSelect)
        val finalList = when {
            filtered.isEmpty() && !policy.allowEmpty && before.isNotEmpty() -> listOf(before.first())
            !policy.allowMultiple && filtered.size > 1 -> listOf(filtered.first())
            else -> filtered.take(policy.maxSelections)
        }
        val added = finalList.filter { a -> before.none { b -> policy.equality(a, b) } }
        val removed = before.filter { b -> finalList.none { a -> policy.equality(a, b) } }
        items.clear(); items.addAll(finalList)
        if (added.isNotEmpty() || removed.isNotEmpty()) notifyListeners(before, added, removed, cause)
    }

    fun select(item: T, cause: String = "select") {
        if (!policy.canSelect(item)) return
        val before = items.toList()
        var changed = false
        if (items.any { existing -> policy.equality(existing, item) }) {
            // already selected: no-op
        } else {
            if (!policy.allowMultiple) {
                val removed = items.toList()
                items.clear()
                if (!policy.allowEmpty) items.add(item) else items.add(item)
                notifyListeners(before, added = listOf(item), removed = removed, cause = cause)
                return
            } else {
                if (items.size < policy.maxSelections) {
                    items.add(item)
                    changed = true
                }
            }
        }
        if (changed) notifyListeners(before, added = listOf(item), removed = emptyList(), cause = cause)
    }

    fun deselect(item: T, cause: String = "deselect") {
        val before = items.toList()
        val idx = items.indexOfFirst { existing -> policy.equality(existing, item) }
        if (idx >= 0) {
            val removedItem = items.removeAt(idx)
            if (!policy.allowEmpty && items.isEmpty()) {
                // Restore the removed item to respect allowEmpty=false
                items.add(removedItem)
                return
            }
            notifyListeners(before, added = emptyList(), removed = listOf(removedItem), cause = cause)
        }
    }

    fun toggle(item: T, cause: String = "toggle") {
        if (items.any { existing -> policy.equality(existing, item) }) deselect(item, cause) else select(item, cause)
    }

    private fun notifyListeners(before: List<T>, added: List<T>, removed: List<T>, cause: String) {
        val event = SelectionChangeEvent(before, added, removed, items.toList(), cause)
        for (l in listeners.toList()) l.onSelectionChanged(event)
    }
}
