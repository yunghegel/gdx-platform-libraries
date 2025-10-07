package dev.jamiecrown.gdx.geometry.core

import dev.jamiecrown.gdx.core.util.Mask
interface Indexed {
    var index: Int
    val flags: Mask
    fun release()
    fun reset()

    companion object {
        const val FLAG_VIRTUAL: Int = 1 shl 31
        const val FLAG_VISITED: Int = 1 shl 30
        const val FLAG_MODIFIED: Int = 1 shl 29
        const val FLAG_SELECTED: Int = 1 shl 28
        const val FLAG_CULLED: Int = 1 shl 27
        const val FLAG_DUPLICATED: Int = 1 shl 26
        const val FLAG_INDEX_MODIFIED: Int = 1 shl 25
    }
}