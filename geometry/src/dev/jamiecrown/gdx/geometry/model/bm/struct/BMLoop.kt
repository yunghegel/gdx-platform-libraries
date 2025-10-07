package dev.jamiecrown.gdx.geometry.model.bm.struct

import dev.jamiecrown.gdx.geometry.core.Element

class BMLoop : Element() {

    var edge: BMEdge? = null
    var face: BMFace? = null
    var vertex: BMVertex? = null
    var nextFaceLoop: BMLoop? = null
    var prevFaceLoop: BMLoop? = null
    var nextEdgeLoop: BMLoop = this
    var prevEdgeLoop: BMLoop = this

    override val kind: Int = BM_LOOP

    override fun release() {
        edge = null
        face = null
        vertex = null
        nextFaceLoop = null
        prevFaceLoop = null
        nextEdgeLoop = this
        prevEdgeLoop = this
    }

    fun faceSetBetween(prev: BMLoop, next: BMLoop) {
        prevFaceLoop = prev
        nextFaceLoop = next
        prev.nextFaceLoop = this
        next.prevFaceLoop = this
    }

    fun faceRemove() {
        if (nextFaceLoop != null && prevFaceLoop != null) {
            nextFaceLoop!!.prevFaceLoop = prevFaceLoop
            prevFaceLoop!!.nextFaceLoop = nextFaceLoop
        } else {
            println("faceRemove: nextFaceLoop or prevFaceLoop is null")
        }
        prevFaceLoop = this
        nextFaceLoop = this
    }


    fun radialSetBetween(prev: BMLoop, next: BMLoop) {
        assert(prev.nextEdgeLoop === next)
        assert(next.prevEdgeLoop === prev)

        prevEdgeLoop = prev
        nextEdgeLoop = next
        prev.nextEdgeLoop = this
        next.prevEdgeLoop = this
    }

    fun radialRemove() {
        nextEdgeLoop.prevEdgeLoop = prevEdgeLoop
        prevEdgeLoop.nextEdgeLoop = nextEdgeLoop
        prevEdgeLoop = this
        nextEdgeLoop = this
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("BMLoop[")
        sb.append("edge: ${edge?.index}, ")
        sb.append("face: ${face?.index}, ")
        sb.append("vertex: ${vertex?.index}, ")
        sb.append("nextFaceLoop: ${nextFaceLoop?.index}, ")
        sb.append("prevFaceLoop: ${prevFaceLoop?.index}, ")
        sb.append("nextEdgeLoop: ${nextEdgeLoop.index}, ")
        sb.append("prevEdgeLoop: ${prevEdgeLoop.index}")
        sb.append("]")
        return sb.toString()
    }


}