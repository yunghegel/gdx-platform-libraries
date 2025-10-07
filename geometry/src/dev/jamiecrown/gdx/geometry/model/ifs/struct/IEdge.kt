package dev.jamiecrown.gdx.geometry.model.ifs.struct

import dev.jamiecrown.gdx.geometry.core.Edge
import kotlin.math.max
import kotlin.math.min

class IEdge : Edge() {
    override val kind: Int = BM_EDGE

    var v0: Int = -1
    var v1: Int = -1

    override fun release() {
        v0 = -1
        v1 = -1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IEdge) return false

        val a0 = min(this.v0, this.v1)
        val a1 = max(this.v0, this.v1)
        val b0 = min(other.v0, other.v1)
        val b1 = max(other.v0, other.v1)

        // If both edges have initialized vertices, compare unordered pairs
        if (a0 >= 0 && a1 >= 0 && b0 >= 0 && b1 >= 0) {
            return a0 == b0 && a1 == b1
        }

        // Fall back to index-based comparison if both initialized
        if (this.index != -1 && other.index != -1) return this.index == other.index

        return false
    }

    override fun hashCode(): Int {
        val a0 = min(this.v0, this.v1)
        val a1 = max(this.v0, this.v1)
        if (a0 >= 0 && a1 >= 0) {
            var h = 17
            h = 31 * h + kind
            h = 31 * h + a0
            h = 31 * h + a1
            return h
        }
        if (index != -1) {
            var h = 17
            h = 31 * h + kind
            h = 31 * h + index
            return h
        }
        return System.identityHashCode(this)
    }
}