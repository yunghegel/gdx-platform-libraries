package dev.jamiecrown.gdx.geometry.model.bm.struct


import dev.jamiecrown.gdx.geometry.model.bm.traversal.EdgeLoopIterator
import dev.jamiecrown.gdx.geometry.model.bm.traversal.LoopMapIterator
import dev.jamiecrown.gdx.geometry.core.Edge
import dev.jamiecrown.gdx.geometry.core.Vertex
import java.util.*


class BMEdge : Edge() {

    override val kind: Int = BM_EDGE

    /**
     * The first adjacent (undirected) [vertex][BMVertex] of this [edge][BMEdge]
     * Never `null` on a valid object.
     */
    var vertex0: BMVertex? = null

    /**
     * The second adjacent (undirected) [vertex][BMVertex] of this [edge][BMEdge].
     * Never `null` on a valid object.
     */
    var vertex1: BMVertex? = null

    /**
     * Disk cycle of [vertex0].
     */

    var v0NextEdge: BMEdge = this
    var v0PrevEdge: BMEdge = this

    /**
     * Disk cycle of [vertex1].
     */

    var v1NextEdge: BMEdge = this
    var v1PrevEdge: BMEdge = this

    /**
     *  Arbitrary [loop][BMLoop] attached to this [edge][BMEdge].
     */

    var loop : BMLoop? = null

    val loops : Iterable<BMLoop>
        get() = Iterable { EdgeLoopIterator(loop) }

    val faces : Iterable<BMFace>
        get() = Iterable {
            LoopMapIterator<BMFace>(
                EdgeLoopIterator(loop)
            ) { loop -> loop.face!! }
        }

    val vertices : List<BMVertex>
        get()  { require(vertex0!=null && vertex1!= null); return listOf(vertex0!!, vertex1!!) }

    override fun release() {
        vertex0 = null
        vertex1 = null
        v0NextEdge = this
        v0PrevEdge = this
        v1NextEdge = this
        v1PrevEdge = this
        loop = null
    }

    /**
     * Adds the specified [BMLoop] to the radial cycle of this [BMEdge].
     *
     * @param loop The [BMLoop] to be added to the radial cycle.
     */

    fun addLoop(loop: BMLoop?) {
        assert(loop?.edge === this)
        if (this.loop == null) {
            this.loop = loop
            return
        }
        // Insert loop at end of linked list
        loop?.radialSetBetween(this.loop!!.prevEdgeLoop, this.loop!!)
    }

    /**
     * Removes the specified [BMLoop] from the radial cycle of this [BMEdge].
     *
     * @param loop The [BMLoop] to be removed from the radial cycle.
     * @throws IllegalArgumentException If the specified loop does not exist in the radial cycle of this [BMEdge].
     */

    fun removeLoop(loop: BMLoop) {
        // Throw NPE if loop is null
        require(loop.edge === this) { "BMLoop is not adjacent to BMEdge" }

        if (this.loop == loop) {
            if (loop.nextEdgeLoop === loop) {
                // BMLoop was the only one here
                this.loop = null
            } else {
                this.loop = loop.nextEdgeLoop
                loop.radialRemove()
            }

            return
        }

        // Check for null so it will throw IllegalArgumentException and not NPE, regardless of this object's state
        if (this.loop != null) {
            // Check if 'loop' exists in radial cycle
            var current: BMLoop = this.loop!!.nextEdgeLoop
            while (current !== this.loop) {
                if (current === loop) {
                    loop.radialRemove()
                    return
                }

                current = current.nextEdgeLoop
            }
        }

        throw IllegalArgumentException("BMLoop does not exist in radial cycle of BMEdge")
    }

    /**
     * Sets the next adjacent [BMEdge] based on the specified contactPoint [BMVertex].
     *
     * @param contactPoint The contactPoint [BMVertex] to set the next adjacency.
     * @param edge         The BMEdge to be set as the next adjacent [BMEdge].
     * @throws IllegalArgumentException If the BMEdge is not adjacent to the contactPoint [BMVertex].
     */
    fun setNextEdge(contactPoint: BMVertex, edge: BMEdge) {
        Objects.requireNonNull(edge)

        if (contactPoint === vertex0) v0NextEdge = edge
        else if (contactPoint === vertex1) v1NextEdge = edge
        else throw java.lang.IllegalArgumentException("BMEdge is not adjacent to BMVertex")
    }

    /**
     * Sets the previous adjacent [BMEdge] based on the specified contactPoint [BMVertex].
     *
     * @param contactPoint The contactPoint BMVertex to set the previous adjacency.
     * @param edge The BMEdge to be set as the previous adjacent [BMEdge].
     * @throws IllegalArgumentException If the BMEdge is not adjacent to the contactPoint [BMVertex].
     */
    fun setPrevEdge(contactPoint: BMVertex, edge: BMEdge) {
        Objects.requireNonNull(edge)

        if (contactPoint === vertex0) v0PrevEdge = edge
        else if (contactPoint === vertex1) v1PrevEdge = edge
        else throw java.lang.IllegalArgumentException("BMEdge is not adjacent to BMVertex")
    }


    /**
     * Returns the next adjacent [BMEdge] based on the specified contactPoint BMVertex.
     *
     * @param contactPoint The contactPoint [BMVertex] to check for adjacency.
     * @return The next BMEdge adjacent to the contactPoint BMVertex.
     * @throws IllegalArgumentException If the [BMEdge] is not adjacent to the contactPoint [BMVertex].
     */
    fun getNextEdge(contactPoint: BMVertex): BMEdge {
        if (contactPoint === vertex0) return v0NextEdge
        else if (contactPoint === vertex1) return v1NextEdge

        throw java.lang.IllegalArgumentException("BMEdge is not adjacent to BMVertex")
    }

    /**
     * Returns the previous adjacent [BMEdge] based on the specified contactPoint [BMVertex].
     *
     * @param contactPoint The contactPoint BMVertex to check for adjacency.
     * @return The previous BMEdge adjacent to the contactPoint [BMVertex].
     * @throws IllegalArgumentException If the [BMEdge] is not adjacent to the contactPoint [BMVertex].
     */
    fun getPrevEdge(contactPoint: BMVertex): BMEdge {
        if (contactPoint === vertex0) return v0PrevEdge
        else if (contactPoint === vertex1) return v1PrevEdge

        throw java.lang.IllegalArgumentException("BMEdge is not adjacent to BMVertex")
    }


    /**
     * Checks if this [edge][BMEdge] connects the specified [vertex][BMVertex] pair, regardless of order.## Heading
     * Note that this edge is undirected.
     */
    fun connects(v0: Vertex?, v1: Vertex?): Boolean {
        if (v0 == null || v1 == null) return false

        return ((vertex0 == v0 && vertex1 == v1)
                || (vertex0 == v1 && vertex1 == v0))
    }

    /**
      Updates the links in the disk cycle of *contactPoint* so that the following order is created:<br></br>
     ```
     Before: prev -> next
     After:  prev -> this -> next
    ```
     * @param contactPoint
     * @param prev
     * @param next
     */
    fun diskSetBetween(contactPoint: BMVertex, prev: BMEdge, next: BMEdge) {
        assert(prev.getNextEdge(contactPoint) === next)
        assert(next.getPrevEdge(contactPoint) === prev)

        if (contactPoint === vertex0) {
            v0NextEdge = next
            v0PrevEdge = prev
            prev.setNextEdge(contactPoint, this)
            next.setPrevEdge(contactPoint, this)
        } else if (contactPoint === vertex1) {
            v1NextEdge = next
            v1PrevEdge = prev
            prev.setNextEdge(contactPoint, this)
            next.setPrevEdge(contactPoint, this)
        } else throw java.lang.IllegalArgumentException("Edge is not adjacent to Vertex")
    }


    /**
     * Removes this Edge from the disk cycle. Links the previous and the next element to each other.
    ```
      Before: prev -> this -> next
      After:  prev -> next
     ```
     * @param contactPoint Adjacent Vertex.
     */
    fun diskRemove(contactPoint: BMVertex) {
        if (contactPoint === vertex0) {
            v0NextEdge.setPrevEdge(contactPoint, v0PrevEdge)
            v0PrevEdge.setNextEdge(contactPoint, v0NextEdge)
            v0NextEdge = this
            v0PrevEdge = this
        } else if (contactPoint === vertex1) {
            v1NextEdge.setPrevEdge(contactPoint, v1PrevEdge)
            v1PrevEdge.setNextEdge(contactPoint, v1NextEdge)
            v1NextEdge = this
            v1PrevEdge = this
        } else throw java.lang.IllegalArgumentException("Edge is not adjacent to Vertex")
    }

    // TODO: It's possible that both vertices are the same - equals() ?
    fun getCommonVertex(other: BMEdge): Vertex? {
        if (vertex0 == other.vertex0 || vertex0 == other.vertex1) return vertex0
        else if (vertex1 == other.vertex0 || vertex1 == other.vertex1) return vertex1

        return null
    }


    fun getOther(vertex: BMVertex): BMVertex {
        return if (vertex === vertex0) vertex1!!
        else if (vertex === vertex1) vertex0!!
        else throw java.lang.IllegalArgumentException("Edge is not adjacent to Vertex")
    }

    fun setOther(contactPoint: BMVertex, vertex: BMVertex) {
        if (contactPoint === vertex0) vertex1 = vertex
        else if (contactPoint === vertex1) vertex0 = vertex
        else throw java.lang.IllegalArgumentException("Edge is not adjacent to Vertex")
    }

    fun replace(oldVertex: BMVertex, newVertex: BMVertex) {
        if (oldVertex === vertex0) vertex0 = newVertex
        else if (oldVertex === vertex1) vertex1 = newVertex
        else throw java.lang.IllegalArgumentException("Edge is not adjacent to Vertex")
    }

    override fun toString(): String {
        return "BMEdge[$vertex0, $vertex1]"
    }





}