package dev.jamiecrown.gdx.geometry.model.he

import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.Vector4
import dev.jamiecrown.gdx.geometry.core.Vertex
import dev.jamiecrown.gdx.geometry.data.ElementData
import dev.jamiecrown.gdx.geometry.data.attribute.Vector2Attribute
import dev.jamiecrown.gdx.geometry.data.attribute.Vector3Attribute
import dev.jamiecrown.gdx.geometry.data.attribute.Vector4Attribute
import dev.jamiecrown.gdx.geometry.model.he.struct.HEdge
import dev.jamiecrown.gdx.geometry.model.he.struct.HFace
import dev.jamiecrown.gdx.geometry.model.he.struct.HVertex

class HalfEdgeMesh {

    var log = true

    val vertices = mutableListOf<HVertex>()
    val faces = mutableListOf<HFace>()
    val halfEdges = mutableListOf<HEdge>()

    private lateinit var gdxMesh: Mesh
    private val vertexBuffer = mutableListOf<Float>()
    private val indexBuffer = mutableListOf<Short>()

    val positionAttribute : Vector3Attribute<HVertex> = Vector3Attribute("a_position")
    val normalAttribute : Vector3Attribute<HVertex> = Vector3Attribute("a_normal")
    val colorAttribute : Vector4Attribute<HVertex> = Vector4Attribute("a_color")
    val uvAttribute: Vector2Attribute<HVertex> = Vector2Attribute("a_uv")

    val vertexData = ElementData  { HVertex() }
    val faceData = ElementData { HFace() }
    val edgeData = ElementData { HEdge() }

    init {
        vertexData.addAttribute(positionAttribute)
        vertexData.addAttribute(normalAttribute)
        vertexData.addAttribute(colorAttribute)
        vertexData.addAttribute(uvAttribute)

    }




    // ... (previous methods remain the same)

    fun importFromMesh(mesh: Mesh) {
        // Clear existing data
        vertices.clear()
        faces.clear()
        halfEdges.clear()
        vertexBuffer.clear()
        indexBuffer.clear()

        // Read vertex data
        val vertexSize = mesh.vertexSize / 4 // Size in floats
        val vertexCount = mesh.numVertices
        val vertexData = FloatArray(vertexCount * vertexSize)
        mesh.getVertices(vertexData)

        val attributes = mesh.vertexAttributes

        for (i in 0 until vertexCount) {
            val vertex = this.vertexData.create()
            vertex.index = i
            vertices.add(vertex)

            for (j in 0 until attributes.size()) {
                val attribute = attributes.get(j)
                val offset = attribute.offset / 4
                val size = attribute.numComponents
                val data = FloatArray(size)
                for (k in 0 until size) {
                    data[k] = vertexData[i * vertexSize + offset + k]
                }
                when (attribute.alias) {
                    "a_position" -> positionAttribute[vertex] = Vector3(data[0], data[1], data[2])
                    "a_normal" -> normalAttribute[vertex] = Vector3(data[0], data[1], data[2])
                    "a_color" -> colorAttribute[vertex] = Vector4(data[0], data[1], data[2], data[3])
                    "a_texCoord0" -> uvAttribute[vertex] = Vector2(data[0], data[1])
                }
            }



        }

        // Read index data
        val indexCount = mesh.numIndices
        val indexData = ShortArray(indexCount)
        mesh.getIndices(indexData)
        println("Index count: $indexCount\n Index data: ${indexData.joinToString()}")

        // Create faces and half-edges
        for (i in 0 until indexCount step 3) {

            val indices = arrayOf(indexData[i], indexData[i + 1], indexData[i + 2])

            if (log) println("Creating face with indices ${indices[0]}, ${indices[1]}, ${indices[2]}")

            val v1 = vertices[indexData[i].toInt()]
            val v2 = vertices[indexData[i + 1].toInt()]
            val v3 = vertices[indexData[i + 2].toInt()]
            createFace(v1, v2, v3)
        }

        // Find pairs for half-edges
        findPairs()

        // Update GDX mesh
        gdxMesh = mesh
        updateGdxMesh()
    }

    private fun updateVertexInBuffer(vertex: HVertex) {
        val index = vertex.index * 3
        if (index + 2 >= vertexBuffer.size) {
            vertexBuffer.addAll(listOf(0f, 0f, 0f))
        }

        val pos = positionAttribute[vertex]

        vertexBuffer[index] = pos.x
        vertexBuffer[index + 1] = pos.y
        vertexBuffer[index + 2] = pos.z

        if (log) println("Updated vertex at $pos with index ${vertex.index}")

    }

    private fun updateIndexBuffer(v1: Vertex, v2: Vertex, v3: Vertex) {
        indexBuffer.addAll(listOf(v1.index.toShort(), v2.index.toShort(), v3.index.toShort()))
    }

    fun updateGdxMesh() {
        if (!::gdxMesh.isInitialized) {
            gdxMesh = Mesh(
                true, vertexBuffer.size / 3, indexBuffer.size,
                VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position")
            )
        }

        gdxMesh.setVertices(vertexBuffer.toFloatArray())
        gdxMesh.setIndices(indexBuffer.toShortArray())
    }

    fun modifyVertex(vertex: HVertex, newPosition: Vector3, autoUpate: Boolean = true) {
        positionAttribute[vertex] = newPosition
        updateVertexInBuffer(vertex)

        if (autoUpate) updateGdxMesh()

        if (log) println("Modified vertex at $newPosition with index ${vertex.index}")

    }

    fun createVertex(position: Vector3): HVertex {

        val vertex =  vertexData.create()
        if (log) println("Created vertex at $position with index ${vertex.index}")


        positionAttribute[vertex] = Vector3(position)

        vertex.index = vertices.size
        positionAttribute[vertex] = position

        vertices.add(vertex)
        updateVertexInBuffer(vertex)


        return vertex
    }

    fun createFace(v1: HVertex, v2: HVertex, v3: HVertex): HFace {
        val face = faceData.create()
        faces.add(face)

        val he1 = edgeData.create()
        he1.face = face
        he1.vertex = v1

        val he2 = edgeData.create()
        he2.face = face
        he2.vertex = v2

        val he3 = edgeData.create()
        he3.face = face
        he3.vertex = v3

        he1.next = he2
        he2.next = he3
        he3.next = he1

        face.halfEdge = he1
        v1.halfEdge = he1
        v2.halfEdge = he2
        v3.halfEdge = he3

        halfEdges.addAll(listOf(he1, he2, he3))

        updateIndexBuffer(v1, v2, v3)

        if (log) println("Created face with vertices ${v1.index}, ${v2.index}, ${v3.index}")

        return face
    }

    fun findPairs() {
        val edgeMap = mutableMapOf<Pair<HVertex, HVertex>, HEdge>()

        for (he in halfEdges) {
            val key = Pair(he.vertex, he.next!!.vertex)
            val reversedKey = Pair(he.next!!.vertex, he.vertex)

            if (reversedKey in edgeMap) {
                val pair = edgeMap[reversedKey]!!
                he.pair = pair
                pair.pair = he
            } else {
                edgeMap[key] = he
            }
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Vertices:\n")
        for (v in vertices) {
            sb.append("  $v\n")
        }

        sb.append("Faces:\n")
        for (f in faces) {
            sb.append("  $f\n")
        }

        sb.append("Half-edges:\n")
        for (he in halfEdges) {
            sb.append("  $he\n")
        }

        return sb.toString()
    }
}