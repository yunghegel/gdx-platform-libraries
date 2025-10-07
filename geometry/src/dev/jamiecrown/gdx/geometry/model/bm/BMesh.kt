package dev.jamiecrown.gdx.geometry.model.bm

import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.math.Vector3
import dev.jamiecrown.gdx.geometry.model.bm.struct.BMEdge
import dev.jamiecrown.gdx.geometry.model.bm.struct.BMFace
import dev.jamiecrown.gdx.geometry.model.bm.struct.BMLoop
import dev.jamiecrown.gdx.geometry.model.bm.struct.BMVertex
import dev.jamiecrown.gdx.geometry.data.ElementData
import dev.jamiecrown.gdx.geometry.data.attribute.AbstractProperty
import dev.jamiecrown.gdx.geometry.data.attribute.FloatAttribute
import dev.jamiecrown.gdx.geometry.data.attribute.FloatTupleAttribute
import dev.jamiecrown.gdx.geometry.data.attribute.ShortTupleAttribute
import dev.jamiecrown.gdx.geometry.data.attribute.Vector2Attribute
import dev.jamiecrown.gdx.geometry.data.attribute.Vector3Attribute
import dev.jamiecrown.gdx.geometry.data.attribute.Vector4Attribute
import java.util.*
import kotlin.collections.ArrayList


class BMesh() {

    internal val uuid = UUID.randomUUID()

    private val tempLoops = ArrayList<BMLoop>()

    val edgeData = ElementData { BMEdge() }
    val loopData = ElementData { BMLoop() }
    val faceData = ElementData { BMFace() }
    val vertexData = ElementData { BMVertex() }

    val edges : Iterable<BMEdge>
        get() = edgeData.elements

    val loops : Iterable<BMLoop>
        get() = loopData.elements

    val faces : Iterable<BMFace>
        get() = faceData.elements

    val vertices : Iterable<BMVertex>
        get() = vertexData.elements

    var vertexSize : Int = 0
        private set

    var mesh: Mesh? = null

    val vertexAttributes : VertexAttributes
        get() {
            val attributes = mutableListOf<VertexAttribute>()
            for (attribute in vertexData.attributes.values) {
                when (attribute.name) {
                    "a_position" -> attributes.add(VertexAttribute(VertexAttributes.Usage.Position, 3, attribute.name))
                    "a_normal" -> attributes.add(VertexAttribute(VertexAttributes.Usage.Normal, 3, attribute.name))
                    "a_color" -> attributes.add(VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, attribute.name))
                    "a_texCoord0" -> attributes.add(VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, attribute.name))
                    else -> {
                        attributes.add(VertexAttribute(VertexAttributes.Usage.Generic, attribute.numComponents, attribute.name))
                    }
                }
            }
            return VertexAttributes(*attributes.toTypedArray())
        }

    fun matchAttribute(attr: VertexAttribute): AbstractProperty<BMVertex, *, *>? {
        return vertexData.attributes.values.firstOrNull { it.name == attr.alias }
    }

    fun initializeMesh(mesh : Mesh) {
        this.mesh = mesh
        val meshAttributes = mesh.vertexAttributes
        meshAttributes.forEach { attr ->
            when (attr.usage) {
                VertexAttributes.Usage.Position -> {
//                    do nothing
                }
                VertexAttributes.Usage.Normal -> {
                    addVertexAttribute(attrNormal)
                }
                VertexAttributes.Usage.ColorPacked -> {
                    addVertexAttribute(attrColor)
                }
                VertexAttributes.Usage.TextureCoordinates -> {
                    addVertexAttribute(attrUV)
                }
                else -> {

                    val attribute = FloatTupleAttribute<BMVertex>(attr.alias, attr.numComponents)
                    addVertexAttribute(attribute)
                }
            }
        }
    }


    val attrPosition = Vector3Attribute<BMVertex>("a_position")
    val attrNormal = Vector3Attribute<BMVertex>("a_normal")
    val attrColor = Vector4Attribute<BMVertex>("a_color")
    val attrUV = Vector2Attribute<BMVertex>("a_texCoord0")

    val attrEdgeIndices: ShortTupleAttribute<BMEdge> = ShortTupleAttribute("a_edgeIndices", 2)

    val attrFaceIndices: ShortTupleAttribute<BMFace> = ShortTupleAttribute("a_faceIndices", 3)

    val offsets: MutableMap<AbstractProperty<BMVertex, *, *>, Int> = mutableMapOf()

    fun addVertexAttribute(attribute: AbstractProperty<BMVertex, *, *>) {
        vertexSize += attribute.numComponents
        offsets[attribute] = vertexData.attributes.values.sumOf { it.numComponents }
        vertexData.addAttribute(attribute)
        println("added attribute ${attribute.name} with offset ${offsets[attribute]}")
    }

    init {
        activeMeshes[uuid] = this
        addVertexAttribute(attrPosition)
        faceData.addAttribute(attrFaceIndices)
        edgeData.addAttribute(attrEdgeIndices)
    }

    fun createVertex(): BMVertex {
        return vertexData.create()
    }

    fun createVertex(x: Float, y: Float, z: Float): BMVertex {
        val vert = createVertex()
        attrPosition[vert] = Vector3(x, y, z)


        return vert
    }

    fun createVertex(location: Vector3): BMVertex {
        val vert = createVertex()
        attrPosition[vert] = location
        return vert
    }

    fun removeVertex(vertex: BMVertex) {
        try {
            assert(tempLoops.isEmpty())

            // Iterate disk cycle for vertex
            for (edge in vertex.edges) {
                // Iterate radial cycle for edge
                for (radialLoop in edge.loops) {
                    // Gather loops and destroy face
                    if (radialLoop.face?.initialized!!) {
                        radialLoop.face!!.collectLoops(tempLoops)
                        faceData.destroy(radialLoop.face!!)
                    }
                }

                edge.getOther(vertex).removeEdge(edge)
                edgeData.destroy(edge)
            }

            for (loop in tempLoops) {
                if (loop.edge!!.initialized) loop.edge!!.removeLoop(loop)
                loopData.destroy(loop)
            }
        } finally {
            tempLoops.clear()
        }

        vertexData.destroy(vertex)
    }


    /**
     * Creates a new edge between the given vertices.
     *
     *
     *  * Create a new edge and associate the vertex pointers with the supplied vertices
     *  * Use the disk cycle of the new edge to populate the pointers for the vertices.
     *
     *
     * @param v0
     * @param v1
     * @return A new edge.
     */
    fun createEdge(v0: BMVertex, v1: BMVertex): BMEdge {
        assert(v0 !== v1)
        val edge: BMEdge = edgeData.create()
        edge.vertex0 = v0
        edge.vertex1 = v1
        v0.addEdge(edge)
        v1.addEdge(edge)

        attrEdgeIndices[edge] = shortArrayOf(v0.index.toShort(), v1.index.toShort())
        return edge
    }


    /**
     * Removes the given [BMEdge] and all adjacent faces from the structure:
     *
     *  * Get loops from adjacent faces by querying the BMEdge's radial cycle.
     *  * Query the loops face to retrieve its membership in neighboring elements.
     *  * Destory that loops face and the other elements attatched to it.
     * @param edge
     */
    fun removeEdge(edge: BMEdge) {
        try {
            // Gather all loops from adjacent faces
            assert(tempLoops.isEmpty())
            for (loop in edge.loops) {
                loop.face!!.collectLoops(tempLoops)
                faceData.destroy(loop.face!!)
            }

            for (loop in tempLoops) {
                loop.edge?.removeLoop(loop)
                loopData.destroy(loop)
            }
        } finally {
            tempLoops.clear()
        }

        edge.vertex0?.removeEdge(edge)
        edge.vertex1?.removeEdge(edge)
        edgeData.destroy(edge)
    }

    /**
     * Creates a new [Face] between the given vertices. The order of the vertices define the winding order of the face.<br></br>
     * If edges between vertices already exist, they are used for the resulting face. Otherwise new edges are created.
     * ----
     * @param faceVertices
     * @return A new Face.
     */
    fun createFace(faceVertices: List<BMVertex>): BMFace {
        require(faceVertices.size >= 3) { "A face needs at least 3 vertices" }

        try {
            assert(tempLoops.isEmpty())
            for (v in faceVertices) {
//                Objects.requireNonNull(v);
                tempLoops.add(loopData.create())
            }

            val face: BMFace = faceData.create()
            face.loop = tempLoops[0]

            var prevLoop: BMLoop? = tempLoops[tempLoops.size - 1]
            var vCurrent = faceVertices[0]

            for (i in faceVertices.indices) {
                val nextIndex = (i + 1) % faceVertices.size
                val vNext = faceVertices[nextIndex]

                var edge: BMEdge? = vCurrent.getEdgeTo(vNext)
                if (edge == null) edge = createEdge(vCurrent, vNext)

                val loop: BMLoop = tempLoops[i]
                loop.face = face
                loop.edge = edge
                loop.vertex = vCurrent
                loop.nextFaceLoop = tempLoops[nextIndex]
                loop.prevFaceLoop = prevLoop
                edge.addLoop(loop)

                prevLoop = loop
                vCurrent = vNext
            }

            attrFaceIndices[face] = shortArrayOf(
                faceVertices[0].index.toShort(),
                faceVertices[1].index.toShort(),
                faceVertices[2].index.toShort()
            )
            return face
        } catch (t: Throwable) {
            for (loop in tempLoops) loopData.destroy(loop)
            throw t
        } finally {
            tempLoops.clear()

        }
    }

    fun createFace(vararg faceVertices: BMVertex): BMFace {
        return createFace(faceVertices.toList())
    }

    /**
     * Splits the edge into two by introducing a new BMVertex (*vNew*):
     *
     *  * Creates a new BMEdge (from *vNew* to *v1*).
     *  * Reference *edge.vertex1* changes from *v1* to *vNew*.
     *  * Updates disk cycle accordingly.
     *  * Adds one additional BMLoop to all adjacent Faces, increasing the number of sides,<br></br>
     * and adds these Loops to the radial cycle of the new BMEdge.
     *
     * ```
     * edge
     * Before: (v0)================(v1)
     * After:  (v0)=====(vNew)-----(v1)
     * edge
     *```
     *
     * @param edge
     * @return A new BMVertex (*vNew*) with undefined attributes (undefined position).
     */
    fun splitEdge(edge: BMEdge): BMVertex {
        // Throws early if edge is null
        val v0: BMVertex? = edge.vertex0
        val v1: BMVertex? = edge.vertex1
        val vNew: BMVertex = vertexData.create()

        val newEdge: BMEdge = edgeData.create()
        newEdge.vertex0 = vNew
        newEdge.vertex1 = v1

        v1?.removeEdge(edge)
        v1?.addEdge(newEdge)

        edge.vertex1 = vNew
        vNew.addEdge(edge)
        vNew.addEdge(newEdge)

        for (loop in edge.loops) {
            val newLoop: BMLoop = loopData.create()
            newLoop.edge = newEdge
            newLoop.face = loop.face
            newEdge.addLoop(newLoop)

            // Link newLoop to next or previous loop, matching winding order.
            if (loop.vertex === v0) {
                // Insert 'newLoop' in front of 'loop'
                // (v0)--loop-->(vNew)--newLoop-->(v1)
                newLoop.faceSetBetween(loop, loop.nextFaceLoop!!)
                newLoop.vertex = vNew
            } else {
                assert(loop.vertex === v1)

                // Insert 'newLoop' at the back of 'loop'
                // (v1)--newLoop-->(vNew)--loop-->(v0)
                newLoop.faceSetBetween(loop.prevFaceLoop!!, loop)
                newLoop.vertex = loop.vertex
                loop.vertex = vNew
            }
        }

        return vNew
    }

    /**
     * Removes the supplied [BMEdge] and [BMVertex] and reconfigures the pointers of the elements they are members of.<br></br>*
     *
     *  * First we need to check a couple things to make the sure the operation is valid.
     *
     *
     *  1. Check if there are exactly two edges in the disk cycle of the vertex
     *  1. Check if there are exactly two edges in the radial cycle of the edge
     *  1. Check if the edge is adjacent to the vertex
     *
     *
     *  * Get the edge we intend to keep from the disk cycle
     *  * Assert that there is an adjacent edge which exists and that there is exactly one other (2 total)
     *  * Remove the edge from the disk cycle of the vertex
     *  * Remove the vertex from the disk cycle of the edge
     *  * Replace the vertex in the edge we intend to keep with the vertex we are removing
     *  * Add the edge to the disk cycle of the vertex we are removing
     *  * Iterate the radial cycle of the edge we are removing
     *  * Remove the face from the face loop
     *  * Cleanup by destroying the face, edge and vertex
     *```
     * Before: (tv)======(v)-----(ov)
     * After:  (tv)--------------(ov)
     *```
     * @param edge Will be removed.
     * @param vertex (*v*) Will be removed.
     * @return True on success.
     */
    fun joinEdge(edge: BMEdge, vertex: BMVertex): Boolean {
        // Do this first so it will throw if edge is null or not adjacent
        val keepEdge: BMEdge = edge.getNextEdge(vertex)

        // No other edges
        if (keepEdge === edge) return false

        // Check if there are >2 edges in disk cycle of vertex
        if (keepEdge.getNextEdge(vertex) !== edge) return false

        val tv: BMVertex = edge.getOther(vertex)
        tv.removeEdge(edge)
        vertex.removeEdge(keepEdge)
        keepEdge.replace(vertex, tv)
        tv.addEdge(keepEdge)

        // Iterate Loops in radial cycle.
        // 'edge' and 'keepEdge' will have same number of loops and they will be connected
        // but the order in the radial cycle can be different (?).
        if (edge.loop != null) {
            var tl: BMLoop = edge.loop!!
            var ol: BMLoop = keepEdge.loop!!

            do {
                if (ol.vertex === vertex) ol.vertex = tv
                ol = ol.nextEdgeLoop

                if (tl.face?.loop === tl) tl.face?.loop = tl.nextFaceLoop

                val loopRemove: BMLoop = tl
                tl = tl.nextEdgeLoop
                loopRemove.faceRemove()
                loopData.destroy(loopRemove)
            } while (tl !== edge.loop)

            assert(ol === keepEdge.loop)
        }

        edgeData.destroy(edge)
        vertexData.destroy(vertex)
        return true
    }

    /**
     * Splits face into two by introducing a new BMEdge (*newEdge*) between the given vertices. Result is two new Faces abd one new BMEdge which is returned.
     * Existing face is on right side, new face will be on left side of new edge (seen from vertex1 while looking at vertex2).
     *
     * @param face
     * @param vertex1
     * @param vertex2
     * @return
     */
    fun splitFace(face: BMFace, vertex1: BMVertex, vertex2: BMVertex): BMEdge {
        assert(vertex1 !== vertex2)

        try {
            assert(tempLoops.isEmpty())

            // Find v2
            var l2: BMLoop? = null
            var loop: BMLoop? = face.loop
            do {
                if (loop?.vertex === vertex2) {
                    l2 = loop
                    break
                }

                loop = loop?.nextFaceLoop
            } while (loop !== face.loop)

            requireNotNull(l2) { "Vertices are not adjacent to the given face" }

            // Continue from v2 and find v1
            var l1: BMLoop? = null
            do {
                if (loop?.vertex === vertex1) {
                    l1 = loop
                    break
                }

                tempLoops.add(loop!!)
                loop = loop.nextFaceLoop
            } while (loop !== l2)

            requireNotNull(l1) { "Vertices are not adjacent to the given face" }

            val newEdge: BMEdge = createEdge(vertex1, vertex2)
            val l1Prev: BMLoop = l1!!.prevFaceLoop!!
            val l2Prev: BMLoop = l2.prevFaceLoop!!

            val l1New: BMLoop = loopData.create()
            l1New.face = face
            l1New.edge = newEdge
            l1New.vertex = vertex2
            l1New.faceSetBetween(l2Prev, l1)

            val newFace: BMFace = faceData.create()
            for (loopF2 in tempLoops) loopF2.face = newFace

            val l2New: BMLoop = loopData.create()
            l2New.face = newFace
            l2New.edge = newEdge
            l2New.vertex = vertex1
            l2New.faceSetBetween(l1Prev, l2)

            face.loop = l1New
            newFace.loop = l2New

            newEdge.addLoop(l1New)
            newEdge.addLoop(l2New)
            return newEdge
        } finally {
            tempLoops.clear()
        }
    }

    fun getData(vertex: BMVertex) : FloatArray {
        val data = FloatArray(vertexData.attributes.values.sumOf { it.numComponents })
        var index = 0
        for (attribute in vertexData.attributes.values.sortedBy { offsets[it] }) {
            val offset = offsets[attribute]!!
            attribute.numComponents
            println(offset)
            val offsetIndex = index
            when (attribute) {
                is Vector3Attribute<BMVertex> -> {

                    val vec = attribute[vertex]
                    data[offsetIndex] = vec.x
                    data[offsetIndex + 1] = vec.y
                    data[offsetIndex + 2] = vec.z
                    index += 3
                }
                is Vector4Attribute<BMVertex> -> {
                    val vec = attribute[vertex]
                    data[offsetIndex] = vec.x
                    data[offsetIndex + 1] = vec.y
                    data[offsetIndex + 2] = vec.z
                    data[offsetIndex + 3] = vec.w
                    index += 4
                }
                is Vector2Attribute<BMVertex> -> {
                    val vec = attribute[vertex]
                    data[offsetIndex] = vec.x
                    data[offsetIndex + 1] = vec.y
                    index += 2
                }
                is FloatAttribute<BMVertex> -> {
                    data[offsetIndex] = attribute[vertex]
                    index += 1
                }
                else -> {
                    throw IllegalArgumentException("Unsupported attribute type")
                }
            }




        }
        return data
    }

    fun toFloatArray(): FloatArray {
        val totalVertices = vertexData.size * (vertexData.attributes.values.sumOf { it.numComponents })
        val floats = FloatArray(totalVertices)
        var index = 0
        for (vertex in vertexData) {
            for (attribute in vertexData.attributes.values) {
                val offset = offsets[attribute]!!
                println("offset: $offset index: $index")
                when (attribute) {
                    is Vector3Attribute<BMVertex> -> {
                        val vec = attribute[vertex]
                        floats[index] = vec.x
                        floats[index + 1] = vec.y
                        floats[index + 2] = vec.z
                        index += 3
                    }
                    is Vector4Attribute<BMVertex> -> {
                        val vec = attribute[vertex]
                        floats[index] = vec.x
                        floats[index + 1] = vec.y
                        floats[index + 2] = vec.z
                        floats[index + 3] = vec.w
                        index += 4
                    }
                    is Vector2Attribute<BMVertex> -> {
                        val vec = attribute[vertex]
                        floats[index] = vec.x
                        floats[index + 1] = vec.y
                        index += 2
                    }
                    is FloatAttribute<BMVertex> -> {
                        floats[index] = attribute[vertex]
                        index += 1
                    }
                }


            }
        }
        return floats

    }

    fun toShortArray(): ShortArray {
        val faces = faceData.map { face ->
            face.collectVertices()
        }
        val shorts = ShortArray(faces.size * 3)
        for (i in faces.indices) {
            val f = faces[i]
            val index = i * 3
            shorts[index] = f[0].index.toShort()
            shorts[index + 1] = f[1].index.toShort()
            shorts[index + 2] = f[2].index.toShort()
        }
        return shorts
    }

    companion object {
        private val activeMeshes = mutableMapOf<UUID, BMesh>()
    }
    

    


}