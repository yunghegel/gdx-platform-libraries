package dev.jamiecrown.gdx.geometry.model.bm.struct

import dev.jamiecrown.gdx.geometry.model.bm.BMesh
import dev.jamiecrown.gdx.geometry.model.bm.traversal.VertexEdgeIterator
import dev.jamiecrown.gdx.geometry.core.Vertex
import kotlin.collections.iterator


class BMVertex : Vertex() {

    override val kind: Int = BM_VERTEX


    /**
     * An arbitrary [BMEdge] that adjacent to this [BMVertex].
     */
    var edge: BMEdge? = null

    override fun release() {
        edge = null
    }

    val edges : Iterable<BMEdge>
        get () = Iterable {
            VertexEdgeIterator(this, edge)
        }

    /**
     * Adds the specified [BMEdge] to the end of the disk cycle of this vertex.
     */

    fun addEdge(edge: BMEdge) {
        assert(edge.getPrevEdge(this) === edge)

        // Insert edge at end of disk cycle
        if (this.edge == null) this.edge = edge
        else edge.diskSetBetween(
            this,
            this.edge!!.getPrevEdge(this), this.edge!!
        )
    }


    /**
     * Removes the specified [BMEdge] from the disk cycle of this vertex.
     *
     * @param edge The edge to be removed from the disk cycle.
     * @throws IllegalArgumentException If the specified edge does not exist in the disk cycle of this vertex.
     */
    fun removeEdge(edge: BMEdge) {
        // Do this first so it will throw if edge is null or not adjacent
        val next: BMEdge = edge.getNextEdge(this)

        if (this.edge == edge) {
            if (next === edge) {
                // BMEdge was the only one in disk cycle
                assert(edge.getPrevEdge(this) === edge)
                this.edge = null
            } else {
                edge.diskRemove(this)
                this.edge = next
            }

            return
        }

        // Check for null so it will throw IllegalArgumentException and not NPE, regardless of this object's state
        if (this.edge != null) {
            // Check if 'edge' exists in disk cycle
            // TODO: Start from 'edge' and check if 'this.edge' is reachable? -> Less iterations?
            //       Or remove this check?
            var current: BMEdge = this.edge!!.getNextEdge(this)
            while (current !== this.edge) {
                if (current === edge) {
                    edge.diskRemove(this)
                    return
                }

                current = current.getNextEdge(this)
            }
        }

        throw IllegalArgumentException("BMEdge does not exist in disk cycle for BMVertex")
    }


    /**
     * Returns the [BMEdge] between this [BMVertex] and the specified [BMVertex].
     *
     * @param other The BMVertex to find the [BMEdge] to.
     * @return The [BMEdge] between this [BMVertex] and the specified [BMVertex], or null if no such [BMEdge] exists.
     */
    fun getEdgeTo(other: BMVertex?): BMEdge? {
        if (edge == null) return null

        var current: BMEdge = this.edge!!
        var iterations = 0
        do {
            iterations++
            if (iterations > 100) return null
            if (current.connects(this, other)) return current
            current = current.getNextEdge(this)
        } while (current !== this.edge)

        return null
    }

    /**
     * If it exists, returns the common [BMFace] between this [BMVertex] and the specified [BMVertex].
     * @param other The BMVertex to find the common face with.
     * @return The common [BMFace] between this [BMVertex] and the specified [BMVertex], or null if no such [BMFace] exists.
     */

    fun getCommonFace(other: BMVertex): BMFace? {
        for (edgeThis in edges) {
            for (loopThis in edgeThis.loops) {
                for (edgeOther in other.edges) {
                    for (loopOther in edgeOther.loops) {
                        if (loopThis.face === loopOther.face) return loopThis.face
                    }
                }
            }
        }
        return null
    }

    fun getAttributeDataString(bmesh: BMesh): String {
        val sb = StringBuilder()
        sb.append("Vertex: $index\n")
        sb.append("Edges: ")
        for (edge in edges) {
            sb.append(edge.index).append(", ")
        }
        sb.append("\n")
        sb.append("Faces: ")
        for (edge in edges) {
            for (loop in edge.loops) {
                sb.append(loop.face!!.index).append(", ")
            }
        }
        sb.append("\n")
        sb.append("Attributes: ")
        for (attr in bmesh.vertexData.attributes) {
            sb.append(attr).append(", ")
        }
        sb.append("\n")
        return sb.toString()
    }

}