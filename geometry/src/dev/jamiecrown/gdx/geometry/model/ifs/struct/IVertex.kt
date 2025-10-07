package dev.jamiecrown.gdx.geometry.model.ifs.struct

import dev.jamiecrown.gdx.geometry.core.Vertex

class IVertex : Vertex() {
    override val kind: Int = BM_VERTEX

    override fun release() {
        // No additional references to clear in IFS
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IVertex) return false

        // If both are initialized, compare by index (stable identity)
        if (this.index != -1 && other.index != -1) return this.index == other.index

        // Otherwise, fall back to reference equality semantics
        return false
    }

    override fun hashCode(): Int {
        // Prefer index when initialized for stable, efficient hashing
        if (index != -1) {
            var h = 17
            h = 31 * h + kind
            h = 31 * h + index
            return h
        }
        // Uninitialized: use identity hash to avoid accidental collisions
        return System.identityHashCode(this)
    }
}