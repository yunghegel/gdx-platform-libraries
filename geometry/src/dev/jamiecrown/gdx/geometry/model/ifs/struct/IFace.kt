package dev.jamiecrown.gdx.geometry.model.ifs.struct

import dev.jamiecrown.gdx.geometry.core.Face

class IFace : Face() {
    override val kind: Int = BM_FACE

    var i0: Int = -1
    var i1: Int = -1
    var i2: Int = -1

    override fun release() {
        i0 = -1
        i1 = -1
        i2 = -1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IFace) return false

        val a = intArrayOf(i0, i1, i2)
        val b = intArrayOf(other.i0, other.i1, other.i2)
        a.sort()
        b.sort()

        // If all initialized, compare unordered triples
        if (a[0] >= 0 && b[0] >= 0) {
            return a[0] == b[0] && a[1] == b[1] && a[2] == b[2]
        }

        // Fall back to index-based comparison if both initialized
        if (this.index != -1 && other.index != -1) return this.index == other.index

        return false
    }

    override fun hashCode(): Int {
        val a = intArrayOf(i0, i1, i2)
        a.sort()
        if (a[0] >= 0) {
            var h = 17
            h = 31 * h + kind
            h = 31 * h + a[0]
            h = 31 * h + a[1]
            h = 31 * h + a[2]
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