package dev.jamiecrown.gdx.geometry.model.bm.struct

import dev.jamiecrown.gdx.geometry.model.bm.traversal.FaceLoopIterator
import dev.jamiecrown.gdx.geometry.model.bm.traversal.LoopMapIterator
import dev.jamiecrown.gdx.geometry.core.Face
import dev.jamiecrown.gdx.geometry.core.Vertex


class BMFace : Face() {

    override val kind: Int = BM_FACE

    var loop : BMLoop? = null

    override fun release() {}

    fun getLoop(from: BMVertex, to: BMVertex): BMLoop? {
        for (loop in loops) {
            if (loop.vertex === from && loop.nextFaceLoop!!.vertex === to) return loop
        }
        return null
    }

    fun commonEdges(face: BMFace): List<BMEdge> {
        val edges: MutableList<BMEdge?> = ArrayList(4)
        for (l1 in loops) {
            for (l2 in face.loops) {
                if (l1.edge === l2.edge) edges.add(loop!!.edge)
            }
        }
        return edges.filterNotNull()
    }

    fun getAnyCommonEdge(face: BMFace): BMEdge? {
        for (l1 in loops) {
            for (l2 in face.loops) {
                if (l1.edge === l2.edge) return loop!!.edge
            }
        }
        return null
    }

    val adjacentFaces : ArrayList<BMFace>
        get() {
            val adjacentFaces = ArrayList<BMFace>()
            for (e in edges) {
                e?.faces?.forEach { f ->
                    if (f !== this) adjacentFaces.add(f)
                }
            }
        return adjacentFaces
    }

    val vertices: Iterable<Vertex>
        get() = Iterable {
            LoopMapIterator<Vertex>(
                FaceLoopIterator(loop!!))
            { loop: BMLoop -> loop.vertex ?: throw IllegalStateException("Loop has undefined vertex") }
        }

    val edges : Iterable<BMEdge?>
        get() =  Iterable {
            LoopMapIterator(
                FaceLoopIterator(loop ?: throw IllegalStateException("Loop is undefined")))
            { loop: BMLoop -> loop.edge }
        }


    val loops : Iterable<BMLoop>
        get() = Iterable { FaceLoopIterator(loop!!) }


    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("BMFace {\n")
        sb.append("  loops: \n")
        for (loop in loops) {
            sb.append("    $loop\n")
        }
        sb.append("}")
        return sb.toString()
    }


}