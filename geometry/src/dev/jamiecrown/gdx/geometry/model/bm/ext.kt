package dev.jamiecrown.gdx.geometry.model.bm

import dev.jamiecrown.gdx.geometry.model.bm.struct.BMEdge
import dev.jamiecrown.gdx.geometry.model.bm.struct.BMFace
import dev.jamiecrown.gdx.geometry.model.bm.struct.BMLoop
import dev.jamiecrown.gdx.geometry.model.bm.struct.BMVertex

fun <C : MutableCollection<BMLoop>> BMFace.collectLoops(collection: C): C {
    var current: BMLoop = loop ?: throw IllegalStateException("Face has no loops")
    do {
        collection.add(current)
        current = current.nextFaceLoop ?: throw IllegalStateException("Loop has no next loop")
    } while (current !== loop)

    return collection
}

fun <C : MutableCollection<BMEdge>> BMFace.collectEdges(collection: C): C {
    for (loop in loops) if (loop.edge != null) collection.add(loop.edge!!)
    return collection
}

fun <C : MutableCollection<BMVertex>> BMFace.collectVertices(collection: C): C {
    for (loop in loops) {
        if (loop.vertex == null) throw IllegalStateException("Loop has undefined vertex")
        collection.add(loop.vertex!!) }
    return collection
}

fun BMFace.collectVertices(): List<BMVertex> = collectVertices(ArrayList())

fun BMFace.collectEdges(): List<BMEdge> = collectEdges(ArrayList())

fun BMFace.collectLoops(): List<BMLoop> = collectLoops(ArrayList())

fun <C : MutableCollection<BMLoop>> BMEdge.collectLoops(collection: C): C {
    for (loop in loops) collection.add(loop)
    return collection
}

fun <C : MutableCollection<BMFace>> BMEdge.collectFaces(collection: C): C {
    for (loop in loops) if (loop.face != null) collection.add(loop.face!!)
    return collection
}

fun BMEdge.collectLoops(): List<BMLoop> = collectLoops(java.util.ArrayList())

fun BMEdge.collectFaces(): List<BMFace> = collectFaces(java.util.ArrayList())

fun BMesh.formatString() : String {
    val sb = StringBuilder()
    sb.append("BMesh {\n")
    sb.append("  vertices: \n${vertexData}\n")
    sb.append("  edges: \n${edgeData}\n")
    sb.append("  faces: \n${faceData}\n")
    sb.append("  loops: \n${loopData}\n")
    sb.append("}")

    return sb.toString()
}