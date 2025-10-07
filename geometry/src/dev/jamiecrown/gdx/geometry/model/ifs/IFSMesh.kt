package dev.jamiecrown.gdx.geometry.model.ifs

import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.math.Vector3
import dev.jamiecrown.gdx.geometry.data.ElementData
import dev.jamiecrown.gdx.geometry.data.attribute.AbstractProperty
import dev.jamiecrown.gdx.geometry.data.attribute.ShortTupleAttribute
import dev.jamiecrown.gdx.geometry.data.attribute.Vector2Attribute
import dev.jamiecrown.gdx.geometry.data.attribute.Vector3Attribute
import dev.jamiecrown.gdx.geometry.data.attribute.Vector4Attribute
import dev.jamiecrown.gdx.geometry.model.ifs.struct.IEdge
import dev.jamiecrown.gdx.geometry.model.ifs.struct.IFace
import dev.jamiecrown.gdx.geometry.model.ifs.struct.IVertex

/**
 * IFSMesh (Indexed Face Set) mirrors the high-level conventions of BMesh but represents
 * a lightweight, array-based mesh where faces are defined by vertex indices.
 *
 * Conventions aligned with BMesh:
 * - Uses ElementData containers for vertices, edges and faces.
 * - Provides default attributes and aliases identical to BMesh (a_position, a_normal, a_color, a_texCoord0).
 * - Exposes vertexAttributes builder compatible with libGDX Mesh.
 * - Provides createVertex/createFace utilities and array export helpers.
 */
class IFSMesh {

    val edgeData = ElementData { IEdge() }
    val faceData = ElementData { IFace() }
    val vertexData = ElementData { IVertex() }

    val edges: Iterable<IEdge>
        get() = edgeData.elements

    val faces: Iterable<IFace>
        get() = faceData.elements

    val vertices: Iterable<IVertex>
        get() = vertexData.elements

    var vertexSize: Int = 0
        private set

    var mesh: Mesh? = null

    val vertexAttributes: VertexAttributes
        get() {
            val attributes = mutableListOf<VertexAttribute>()
            for (attribute in vertexData.attributes.values) {
                when (attribute.name) {
                    "a_position" -> attributes.add(VertexAttribute(VertexAttributes.Usage.Position, 3, attribute.name))
                    "a_normal" -> attributes.add(VertexAttribute(VertexAttributes.Usage.Normal, 3, attribute.name))
                    "a_color" -> attributes.add(VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, attribute.name))
                    "a_texCoord0" -> attributes.add(VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, attribute.name))
                    else -> attributes.add(VertexAttribute(VertexAttributes.Usage.Generic, attribute.numComponents, attribute.name))
                }
            }
            return VertexAttributes(*attributes.toTypedArray())
        }

    fun matchAttribute(attr: VertexAttribute): AbstractProperty<IVertex, *, *>? {
        return vertexData.attributes.values.firstOrNull { it.name == attr.alias }
    }

    fun initializeMesh(mesh: Mesh) {
        this.mesh = mesh
        val meshAttributes = mesh.vertexAttributes
        meshAttributes.forEach { attr ->
            when (attr.usage) {
                VertexAttributes.Usage.Position -> {
                    // position always present by default
                }
                VertexAttributes.Usage.Normal -> addVertexAttribute(attrNormal)
                VertexAttributes.Usage.ColorPacked -> addVertexAttribute(attrColor)
                VertexAttributes.Usage.TextureCoordinates -> addVertexAttribute(attrUV)
                else -> {
                    // For any unknown attribute, allocate a generic float tuple with the same alias and component count
                    addVertexAttribute(dev.jamiecrown.gdx.geometry.data.attribute.FloatTupleAttribute<IVertex>(attr.alias, attr.numComponents))
                }
            }
        }
    }

    fun loadFromMesh(mesh: Mesh) {
        initializeMesh(mesh)
        val vertexCount = mesh.numVertices
        val vertexSize = mesh.vertexAttributes.vertexSize / 4 // in floats
        val vertices = FloatArray(vertexCount * vertexSize)
        mesh.getVertices(vertices)
        for (i in 0 until vertexCount) {
            val v = createVertex()
            var offset = i * vertexSize
            for (attribute in vertexData.attributes.values) {
                @Suppress("UNCHECKED_CAST")
                val prop = attribute as AbstractProperty<IVertex, Any, Any>
                when (prop.numComponents) {
                    2 -> prop[v] = floatArrayOf(vertices[offset], vertices[offset + 1])
                    3 -> prop[v] = Vector3(vertices[offset], vertices[offset + 1], vertices[offset + 2])
                    4 -> prop[v] = floatArrayOf(vertices[offset], vertices[offset + 1], vertices[offset + 2], vertices[offset + 3])
                    else -> {
                        // For generic attributes, read as float array
                        val arr = FloatArray(prop.numComponents)
                        for (j in 0 until prop.numComponents) {
                            arr[j] = vertices[offset + j]
                        }
                        prop[v] = arr
                    }
                }
                offset += prop.numComponents
            }
        }
        // Note: Mesh indices are not loaded into IFSMesh faces/edges automatically
    }

    // Default attributes aligned with BMesh
    val attrPosition = Vector3Attribute<IVertex>("a_position")
    val attrNormal = Vector3Attribute<IVertex>("a_normal")
    val attrColor = Vector4Attribute<IVertex>("a_color")
    val attrUV = Vector2Attribute<IVertex>("a_texCoord0")

    val attrEdgeIndices: ShortTupleAttribute<IEdge> = ShortTupleAttribute("a_edgeIndices", 2)
    val attrFaceIndices: ShortTupleAttribute<IFace> = ShortTupleAttribute("a_faceIndices", 3)

    val offsets: MutableMap<AbstractProperty<IVertex, *, *>, Int> = mutableMapOf()

    fun addVertexAttribute(attribute: AbstractProperty<IVertex, *, *>) {
        vertexSize += attribute.numComponents
        offsets[attribute] = vertexData.attributes.values.sumOf { it.numComponents }
        vertexData.addAttribute(attribute)
    }

    init {
        // Position is always present
        addVertexAttribute(attrPosition)
        // Index-like attributes on faces/edges for convenience, similar to BMesh
        faceData.addAttribute(attrFaceIndices)
        edgeData.addAttribute(attrEdgeIndices)
    }

    fun createVertex(): IVertex {
        return vertexData.create()
    }

    fun createVertex(x: Float, y: Float, z: Float): IVertex {
        val v = createVertex()
        attrPosition[v] = Vector3(x, y, z)
        return v
    }

    fun createVertex(location: Vector3): IVertex {
        val v = createVertex()
        attrPosition[v] = location
        return v
    }

    fun createEdge(v0: IVertex, v1: IVertex): IEdge {
        val e = edgeData.create()
        // store indices in attribute for quick export/debug; topology links are intentionally omitted in IFS
        attrEdgeIndices[e] = shortArrayOf(v0.index.toShort(), v1.index.toShort())
        return e
    }

    fun createFace(v0: IVertex, v1: IVertex, v2: IVertex): IFace {
        val f = faceData.create()
        attrFaceIndices[f] = shortArrayOf(v0.index.toShort(), v1.index.toShort(), v2.index.toShort())
        // Optionally register edges (no deduplication here to keep this minimal)
        createEdge(v0, v1)
        createEdge(v1, v2)
        createEdge(v2, v0)
        return f
    }

    fun createFace(vertices: List<IVertex>): IFace {
        require(vertices.size >= 3) { "A face needs at least 3 vertices" }
        return createFace(vertices[0], vertices[1], vertices[2])
    }

    fun toFloatArray(): FloatArray {
        // Pack vertex attribute data interleaved in the order attributes are registered
        val count = vertexData.size
        val stride = vertexData.attributes.values.sumOf { it.numComponents }
        val out = FloatArray(maxOf(0, count * stride))
        var write = 0
        for (v in vertexData) {
            for (attribute in vertexData.attributes.values) {
                @Suppress("UNCHECKED_CAST")
                val prop = attribute as AbstractProperty<IVertex, Any, Any>
                when (val data = prop[v]) {
                    is FloatArray -> {
                        var i = 0
                        while (i < prop.numComponents) {
                            out[write++] = data[i]
                            i++
                        }
                    }
                    is Vector3 -> {
                        out[write++] = data.x
                        out[write++] = data.y
                        out[write++] = data.z
                    }
                    is com.badlogic.gdx.math.Vector2 -> {
                        out[write++] = data.x
                        out[write++] = data.y
                    }
                    is com.badlogic.gdx.math.Vector4 -> {
                        out[write++] = data.x
                        out[write++] = data.y
                        out[write++] = data.z
                        out[write++] = data.w
                    }
                    else -> {
                        // For generic attributes that store primitives as arrays
                        if (data is Array<*>) {
                            for (i in 0 until prop.numComponents) {
                                val value = data[i]
                                out[write++] = when (value) {
                                    is Number -> value.toFloat()
                                    else -> 0f
                                }
                            }
                        } else {
                            // If attribute value is unset, fill zeros
                            repeat(prop.numComponents) { out[write++] = 0f }
                        }
                    }
                }
            }
        }
        return out
    }

    fun toShortArray(): ShortArray {
        // Triangles only from attrFaceIndices
        val out = ShortArray(faceData.size * 3)
        var i = 0
        for (f in faceData) {
            val idx = attrFaceIndices[f]
            out[i++] = idx[0]
            out[i++] = idx[1]
            out[i++] = idx[2]
        }
        return out
    }
}