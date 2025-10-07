package dev.jamiecrown.gdx.geometry

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.GdxRuntimeException
import dev.jamiecrown.gdx.geometry.core.Edge
import dev.jamiecrown.gdx.geometry.core.Face
import dev.jamiecrown.gdx.geometry.core.Indexed
import dev.jamiecrown.gdx.geometry.core.Vertex
import dev.jamiecrown.gdx.geometry.model.ifs.struct.IEdge
import dev.jamiecrown.gdx.geometry.model.ifs.struct.IFace
import dev.jamiecrown.gdx.geometry.model.ifs.struct.IVertex
import dev.jamiecrown.gdx.geometry.model.ifs.IFSMesh
import dev.jamiecrown.gdx.geometry.model.bm.BMesh
import dev.jamiecrown.gdx.geometry.model.bm.GdxMesh
import dev.jamiecrown.gdx.geometry.model.he.HalfEdgeMesh
import org.lwjgl.opengl.GL40
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

/**
 * PrimitiveType codes encoded into 24-bit color:
 *   0 -> none
 *   1 -> vertex
 *   2 -> edge
 *   3 -> face
 *
 * We pack (typeCode << 22) | (localIndex + 1) into a 24-bit integer
 * so we can decode both the type and the local primitive index.
 * This yields 2 bits for type (up to 4 types) and 22 bits for index (~4.19M).
 */
private object PickEncoding {
    const val TYPE_SHIFT = 22
    const val INDEX_MASK = (1 shl TYPE_SHIFT) - 1 // lower 22 bits

    fun encode(typeCode: Int, localIndexZeroBased: Int): Int {
        require(typeCode in 1..3) { "typeCode must be 1..3" }
        val id = ((typeCode and 0x3) shl TYPE_SHIFT) or ((localIndexZeroBased + 1) and INDEX_MASK)
        // id is 24-bit (fits into RGB)
        return id and 0xFFFFFF
    }

    fun encodeToColorFloats(typeCode: Int, localIndexZeroBased: Int): FloatArray {
        val id = encode(typeCode, localIndexZeroBased)
        val r = (id shr 16) and 0xFF
        val g = (id shr 8) and 0xFF
        val b = id and 0xFF
        return floatArrayOf(r / 255f, g / 255f, b / 255f, 1f)
    }

    fun decodeFromBytes(r: Int, g: Int, b: Int): PickResult? {
        val id = (r shl 16) or (g shl 8) or b
        if (id == 0) return null
        val typeCode = (id shr TYPE_SHIFT) and 0x3
        val localIndexPlusOne = id and INDEX_MASK
        if (localIndexPlusOne == 0) return null
        val localIndex = localIndexPlusOne - 1
        val type = when (typeCode) {
            1 -> PrimitiveType.Vertex
            2 -> PrimitiveType.Edge
            3 -> PrimitiveType.Face
            else -> return null
        }
        return PickResult(type, localIndex)
    }
}

enum class PrimitiveType { Vertex, Edge, Face }

/** Result from a pick query */
data class PickResult(val primitiveType: PrimitiveType, val primitiveIndex: Int)

/**
 * Main renderer:
 *
 *  - buildFromMesh(...) constructs both visible wireline mesh and per-type picking meshes.
 *  - render(...) does visible drawing (single mesh for wireframe).
 *  - pick(x,y, camera) renders to an offscreen FBO (if necessary) and returns decoded PickResult.
 *
 * Requirements from caller:
 *  - Provide lists/arrays mapping primitive localIndex -> Indexed object if you want direct object lookup.
 *    The renderer itself only returns (type, index). It's expected your app maps that index to a concrete object.
 */
class WireframePickerRenderer(
    private val maxPickWidth: Int = 2048,
    private val maxPickHeight: Int = 2048
) {
    // --- visible line mesh (positions only) ---
    private var visibleLineMesh: Mesh? = null
    private var visibleLineIndexCount = 0

    // --- pick meshes: position + color attribute (RGBA as floats) ---
    private var pickVertexMesh: Mesh? = null
    private var pickEdgeMesh: Mesh? = null
    private var pickFaceMesh: Mesh? = null
    private var pickVertexIndexCount = 0
    private var pickEdgeIndexCount = 0
    private var pickFaceIndexCount = 0

    // --- simple shader for visible wireframe (like ShapeRenderer) ---
    private val visibleShader: ShaderProgram

    // --- shader that outputs vertex color directly (used for picking) ---
    private val pickShader: ShaderProgram

    // Offscreen FBO used for picking
    private var pickFbo: FrameBuffer? = null
    private var pickFboWidth = 0
    private var pickFboHeight = 0

    // --- debug view settings ---
    var debugDrawFaces: Boolean = true
    var debugDrawEdges: Boolean = true
    var debugDrawVertices: Boolean = true
    var debugLineWidth: Float = 2f
    var debugPointSize: Float = 6f

    init {
        ShaderProgram.pedantic = false

        visibleShader = ShaderProgram(
            // vertex: position only + uniform mat
            """
            attribute vec4 a_position;
            uniform mat4 u_projModelView;
            void main() {
              gl_Position = u_projModelView * a_position;
            }
            """.trimIndent(),
            // fragment: uniform color
            """
            #ifdef GL_ES
            precision mediump float;
            #endif
            uniform vec4 u_color;
            void main() {
              gl_FragColor = u_color;
            }
            """.trimIndent()
        )
        if (!visibleShader.isCompiled) {
            throw GdxRuntimeException("Visible shader compile failed: ${visibleShader.log}")
        }

        pickShader = ShaderProgram(
            // vertex: position and color attribute forwarded
            """
            attribute vec4 a_position;
            attribute vec4 a_color;
            varying vec4 v_color;
            uniform mat4 u_projModelView;
            void main() {
              v_color = a_color;
              gl_Position = u_projModelView * a_position;
              #ifdef GL_ES
                gl_PointSize = 6.0; // if drawing points for vertex picks, adjustable
              #endif
            }
            """.trimIndent(),
            // frag: output varying color (the encoded id)
            """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec4 v_color;
            void main() {
              gl_FragColor = v_color;
            }
            """.trimIndent()
        )
        if (!pickShader.isCompiled) {
            throw GdxRuntimeException("Pick shader compile failed: ${pickShader.log}")
        }
    }

    /**
     * Build caches (visible and pick) from a source Mesh provided as an Indexed Face List (triangles),
     * plus the corresponding Indexed-lists for vertices/edges/faces so we can assign pick ids.
     *
     * - source: LibGDX Mesh (triangles). Must have vertex positions accessible via VertexAttributes.Usage.Position
     * - verticesList: list mapping vertexIndex -> Indexed
     * - edgesList: list mapping edgeIndex -> Indexed  (edge index order must correspond to how you want local indices)
     * - facesList: list mapping faceIndex -> Indexed  (face index order must correspond to triangle order in indices)
     *
     * Note: facesList.size is expected to equal source.numIndices/3 (each triangle -> one face entry).
     *
     * This method:
     *  - extracts the source positions & indices once
     *  - builds a visible lines mesh (unique edges)
     *  - builds pick meshes:
     *      * vertices: GL_POINTS, one colored vertex per source vertex (color encodes (type=1, index))
     *      * edges: GL_LINES, each edge rendered with uniform encoded color
     *      * faces: GL_TRIANGLES, each triangle's 3 verts colored with the same encoded color
     *
     * For memory efficiency we create float arrays sized to fit the primitives and create Mesh objects once.
     */
    fun buildFromIFSMesh(ifs: IFSMesh) {
        // Build a libGDX mesh from IFSMesh data, then delegate
        val vCount = (ifs.vertices as Iterable<IVertex>).count()
        val fCount = (ifs.faces as Iterable<IFace>).count()
        val mesh = Mesh(true, vCount, fCount * 3, ifs.vertexAttributes)
        mesh.setVertices(ifs.toFloatArray())
        mesh.setIndices(ifs.toShortArray())
        val verticesList: List<IVertex> = ifs.vertices.toList()
        val edgesList: List<IEdge> = ifs.edges.toList()
        val facesList: List<IFace> = ifs.faces.toList()
        buildFromMesh(mesh, verticesList, edgesList, facesList)
    }

    fun buildFromBMesh(bmesh: BMesh) {
        val mesh = GdxMesh.convertFrom(bmesh)
        val verticesList = bmesh.vertices.toList()
        val edgesList = bmesh.edges.toList()
        val facesList = bmesh.faces.toList()
        buildFromMesh(mesh, verticesList, edgesList, facesList)
    }

    fun buildFromHalfEdgeMesh(he: HalfEdgeMesh, sourceMesh: Mesh) {
        // HalfEdgeMesh manages its own topological structures; we accept an externally supplied Mesh
        val verticesList = he.vertices.toList() // List<HVertex> : Vertex
        val edgesList = he.halfEdges.toList()  // List<HEdge> : Edge
        val facesList = he.faces.toList()      // List<HFace> : Face
        buildFromMesh(sourceMesh, verticesList, edgesList, facesList)
    }

    fun buildFromMesh(
        source: Mesh,
        verticesList: List<out Indexed>,
        edgesList: List<out Indexed>,
        facesList: List<out Indexed>
    ) {
        // --- read source arrays once ---
        val stride = source.vertexSize / 4
        val posOffset = source.getVertexAttribute(VertexAttributes.Usage.Position).offset / 4
        val numVerts = source.numVertices
        val numIndices = source.numIndices

        val srcVerts = FloatArray(numVerts * stride)
        val srcIndices = ShortArray(numIndices)
        source.getVertices(srcVerts)
        source.getIndices(srcIndices)

        // --- build unique edge set from triangle list ---
        val edgeSet = LinkedHashSet<Pair<Int, Int>>() // preserve insertion order for consistent indices
        val triangleCount = numIndices / 3
        for (t in 0 until triangleCount) {
            val a = srcIndices[t * 3].toInt() and 0xFFFF
            val b = srcIndices[t * 3 + 1].toInt() and 0xFFFF
            val c = srcIndices[t * 3 + 2].toInt() and 0xFFFF
            fun addEdge(i1: Int, i2: Int) {
                val p = if (i1 < i2) i1 to i2 else i2 to i1
                edgeSet.add(p)
            }
            addEdge(a, b); addEdge(b, c); addEdge(c, a)
        }

        // --- Prepare visible line mesh data (positions only) ---
        val lineVertexCount = edgeSet.size * 2
        val visiblePositions = FloatArray(lineVertexCount * 3) // x,y,z only
        var vposIdx = 0
        for ((a, b) in edgeSet) {
            val va = a * stride + posOffset
            val vb = b * stride + posOffset
            visiblePositions[vposIdx++] = srcVerts[va]
            visiblePositions[vposIdx++] = srcVerts[va + 1]
            visiblePositions[vposIdx++] = srcVerts[va + 2]
            visiblePositions[vposIdx++] = srcVerts[vb]
            visiblePositions[vposIdx++] = srcVerts[vb + 1]
            visiblePositions[vposIdx++] = srcVerts[vb + 2]
        }
        // Indices for lines (0..N-1 sequential)
        val visibleIndices = ShortArray(lineVertexCount) { it.toShort() }
        visibleLineMesh?.dispose()
        visibleLineMesh = Mesh(true, visiblePositions.size / 3, visibleIndices.size,
            VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position")
        )
        visibleLineMesh!!.setVertices(visiblePositions)
        visibleLineMesh!!.setIndices(visibleIndices)
        visibleLineIndexCount = visibleIndices.size

        // --- Build pick meshes ---
        // 1) vertex pick mesh (GL_POINTS) - each vertex colored with its encoded id
        val vertexPickPositions = FloatArray(numVerts * 3)
        val vertexPickColors = FloatArray(numVerts * 4)
        var vi = 0
        for (i in 0 until numVerts) {
            val posIdx = i * stride + posOffset
            vertexPickPositions[vi * 3 + 0] = srcVerts[posIdx]
            vertexPickPositions[vi * 3 + 1] = srcVerts[posIdx + 1]
            vertexPickPositions[vi * 3 + 2] = srcVerts[posIdx + 2]
            val idColor = PickEncoding.encodeToColorFloats(1, i) // typeCode 1 = vertex
            vertexPickColors[vi * 4 + 0] = idColor[0]
            vertexPickColors[vi * 4 + 1] = idColor[1]
            vertexPickColors[vi * 4 + 2] = idColor[2]
            vertexPickColors[vi * 4 + 3] = 1f
            vi++
        }
        // point indices sequential
        val vertexPickIndices = ShortArray(numVerts) { it.toShort() }

        pickVertexMesh?.dispose()
        // mesh attributes: position + color
        pickVertexMesh = Mesh(true, numVerts, numVerts,
            VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color")
        )
        // vertex interleaving: libGDX .setVertices expects interleaved attributes
        // Build interleaved array of [pos.x,pos.y,pos.z, color.r,g,b,a] per vertex
        val vertexPickInterleaved = FloatArray(numVerts * (3 + 4))
        for (i in 0 until numVerts) {
            val baseSrc = i * 3
            val baseDst = i * 7
            vertexPickInterleaved[baseDst + 0] = vertexPickPositions[baseSrc + 0]
            vertexPickInterleaved[baseDst + 1] = vertexPickPositions[baseSrc + 1]
            vertexPickInterleaved[baseDst + 2] = vertexPickPositions[baseSrc + 2]
            vertexPickInterleaved[baseDst + 3] = vertexPickColors[baseSrc + 0]
            vertexPickInterleaved[baseDst + 4] = vertexPickColors[baseSrc + 1]
            vertexPickInterleaved[baseDst + 5] = vertexPickColors[baseSrc + 2]
            vertexPickInterleaved[baseDst + 6] = vertexPickColors[baseSrc + 3]
        }
        pickVertexMesh!!.setVertices(vertexPickInterleaved)
        pickVertexMesh!!.setIndices(vertexPickIndices)
        pickVertexIndexCount = vertexPickIndices.size

        // 2) edge pick mesh (GL_LINES) - each line's two vertices colored with encoded edge id
        val edgeCount = edgeSet.size
        val edgePickPositions = FloatArray(edgeCount * 2 * 3)
        val edgePickColors = FloatArray(edgeCount * 2 * 4)
        val edgePickIndices = ShortArray(edgeCount * 2)
        var edgeIdx = 0
        var evi = 0
        for ((a, b) in edgeSet) {
            val va = a * stride + posOffset
            val vb = b * stride + posOffset
            // first endpoint
            edgePickPositions[evi * 3 + 0] = srcVerts[va]
            edgePickPositions[evi * 3 + 1] = srcVerts[va + 1]
            edgePickPositions[evi * 3 + 2] = srcVerts[va + 2]
            val colorA = PickEncoding.encodeToColorFloats(2, edgeIdx) // typeCode 2 = edge
            edgePickColors[evi * 4 + 0] = colorA[0]
            edgePickColors[evi * 4 + 1] = colorA[1]
            edgePickColors[evi * 4 + 2] = colorA[2]
            edgePickColors[evi * 4 + 3] = 1f
            edgePickIndices[evi] = evi.toShort()
            evi++

            // second endpoint
            edgePickPositions[evi * 3 + 0] = srcVerts[vb]
            edgePickPositions[evi * 3 + 1] = srcVerts[vb + 1]
            edgePickPositions[evi * 3 + 2] = srcVerts[vb + 2]
            val colorB = PickEncoding.encodeToColorFloats(2, edgeIdx)
            edgePickColors[evi * 4 + 0] = colorB[0]
            edgePickColors[evi * 4 + 1] = colorB[1]
            edgePickColors[evi * 4 + 2] = colorB[2]
            edgePickColors[evi * 4 + 3] = 1f
            edgePickIndices[evi] = evi.toShort()
            evi++
            edgeIdx++
        }
        pickEdgeMesh?.dispose()
        pickEdgeMesh = Mesh(true, edgePickPositions.size / 3, edgePickIndices.size,
            VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color")
        )
        // interleave pos+color
        val edgeInterleaved = FloatArray(edgePickPositions.size / 3 * 7)
        var srcVertexCount = edgePickPositions.size / 3
        for (i in 0 until srcVertexCount) {
            val pbase = i * 3
            val dbase = i * 7
            edgeInterleaved[dbase + 0] = edgePickPositions[pbase + 0]
            edgeInterleaved[dbase + 1] = edgePickPositions[pbase + 1]
            edgeInterleaved[dbase + 2] = edgePickPositions[pbase + 2]
            edgeInterleaved[dbase + 3] = edgePickColors[pbase + 0]
            edgeInterleaved[dbase + 4] = edgePickColors[pbase + 1]
            edgeInterleaved[dbase + 5] = edgePickColors[pbase + 2]
            edgeInterleaved[dbase + 6] = edgePickColors[pbase + 3]
        }
        pickEdgeMesh!!.setVertices(edgeInterleaved)
        pickEdgeMesh!!.setIndices(edgePickIndices)
        pickEdgeIndexCount = edgePickIndices.size

        // 3) face pick mesh (GL_TRIANGLES) - each triangle's three vertices colored with same encoded face id
        // facesList is expected to correspond to triangles in source (one face per triangle). If facesList size differs, we still assign by triangle index.
        val triCount = triangleCount
        val facePickPositions = FloatArray(triCount * 3 * 3) // triCount * 3 verts * 3 coords
        val facePickColors = FloatArray(triCount * 3 * 4)
        val facePickIndices = ShortArray(triCount * 3)
        var fiVert = 0
        for (t in 0 until triCount) {
            val aIdx = srcIndices[t * 3].toInt() and 0xFFFF
            val bIdx = srcIndices[t * 3 + 1].toInt() and 0xFFFF
            val cIdx = srcIndices[t * 3 + 2].toInt() and 0xFFFF
            val positions = intArrayOf(aIdx, bIdx, cIdx)
            val colorFloats = PickEncoding.encodeToColorFloats(3, t) // typeCode 3 = face
            for (corner in 0 until 3) {
                val srcPos = positions[corner] * stride + posOffset
                facePickPositions[fiVert * 3 + 0] = srcVerts[srcPos]
                facePickPositions[fiVert * 3 + 1] = srcVerts[srcPos + 1]
                facePickPositions[fiVert * 3 + 2] = srcVerts[srcPos + 2]
                facePickColors[fiVert * 4 + 0] = colorFloats[0]
                facePickColors[fiVert * 4 + 1] = colorFloats[1]
                facePickColors[fiVert * 4 + 2] = colorFloats[2]
                facePickColors[fiVert * 4 + 3] = colorFloats[3]
                facePickIndices[fiVert] = fiVert.toShort()
                fiVert++
            }
        }
        pickFaceMesh?.dispose()
        pickFaceMesh = Mesh(true, facePickPositions.size / 3, facePickIndices.size,
            VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color")
        )
        // interleave
        val faceInterleaved = FloatArray(facePickPositions.size / 3 * 7)
        val faceVertexCount = facePickPositions.size / 3
        for (i in 0 until faceVertexCount) {
            val pbase = i * 3
            val dbase = i * 7
            faceInterleaved[dbase + 0] = facePickPositions[pbase + 0]
            faceInterleaved[dbase + 1] = facePickPositions[pbase + 1]
            faceInterleaved[dbase + 2] = facePickPositions[pbase + 2]
            faceInterleaved[dbase + 3] = facePickColors[pbase + 0]
            faceInterleaved[dbase + 4] = facePickColors[pbase + 1]
            faceInterleaved[dbase + 5] = facePickColors[pbase + 2]
            faceInterleaved[dbase + 6] = facePickColors[pbase + 3]
        }
        pickFaceMesh!!.setVertices(faceInterleaved)
        pickFaceMesh!!.setIndices(facePickIndices)
        pickFaceIndexCount = facePickIndices.size

        // Done. Note: we don't store references to the Indexed objects inside the class; caller maps the result index to the objects as required.
        // If you want, you can keep lists here as well for convenience (not included to keep this generic).
    }

    /**
     * Render visible wireframe (single draw call for line mesh).
     * Use camera.combined as projModelView matrix and a color for the wireframe.
     */
    fun renderVisible(projModelView: Matrix4, color: Color = Color.WHITE, lineWidth: Float = 1f) {
        val mesh = visibleLineMesh ?: return
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glLineWidth(lineWidth)
        visibleShader.bind()
        visibleShader.setUniformMatrix("u_projModelView", projModelView)
        visibleShader.setUniformf("u_color", color.r, color.g, color.b, color.a)
        mesh.render(visibleShader, GL20.GL_LINES, 0, visibleLineIndexCount)
    }

    /**
     * Debug rendering: draw the pick meshes directly to the default framebuffer so you can see the encoded colors.
     * Useful to validate picking id assignment and occlusion ordering.
     */
    fun renderPickDebug(
        projModelView: Matrix4,
        drawFaces: Boolean = debugDrawFaces,
        drawEdges: Boolean = debugDrawEdges,
        drawVertices: Boolean = debugDrawVertices,
        lineWidth: Float = debugLineWidth,
        pointSize: Float = debugPointSize
    ) {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glLineWidth(lineWidth)
        pickShader.bind()
        pickShader.setUniformMatrix("u_projModelView", projModelView)
        // Faces first
//        if (drawFaces) {
//            pickFaceMesh?.let { it.render(pickShader, GL20.GL_TRIANGLES, 0, pickFaceIndexCount) }
//        }
        // Then edges
        if (drawEdges) {
            pickEdgeMesh?.let { it.render(pickShader, GL20.GL_LINES, 0, pickEdgeIndexCount) }
        }
        // Then vertices
        if (drawVertices) {
            // For desktop GL allow programmable point size
            Gdx.gl.glEnable(GL40.GL_PROGRAM_POINT_SIZE)
            // Note: gl_PointSize is set in vertex shader (constant). To change dynamically, update shader to use a uniform.
            pickVertexMesh?.let { it.render(pickShader, GL20.GL_POINTS, 0, pickVertexIndexCount) }
            Gdx.gl.glDisable(GL40.GL_PROGRAM_POINT_SIZE)
        }
    }

    /**
     * Perform a pick query at window coordinates (x,y) where origin (0,0) is bottom-left.
     * - cameraCombined: matrix to use for rendering into the pick FBO (camera.combined)
     * - pickWidth/Height: optional override for FBO resolution (useful for high-DPI rendering)
     *
     * Returns a PickResult (type + local index) or null if nothing hit.
     */
    fun pickAt(x: Int, y: Int, cameraCombined: Matrix4, pickWidth: Int = 0, pickHeight: Int = 0): PickResult? {
        // Lazy init / resize FBO
        val fbw = if (pickWidth <= 0) max(1, Gdx.graphics.width) else pickWidth
        val fbh = if (pickHeight <= 0) max(1, Gdx.graphics.height) else pickHeight
        ensurePickFbo(w = minOf(fbw, maxPickWidth), h = minOf(fbh, maxPickHeight))

        val fbo = pickFbo ?: return null

        // Bind FBO and render pick meshes (faces, edges, vertices).
        fbo.begin()
        // Clear to zero (0,0,0) -> no hit
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // Render faces first (so they can occlude edges/verts)
        pickShader.bind()
        pickShader.setUniformMatrix("u_projModelView", cameraCombined)
        // -- faces (triangles) --
        pickFaceMesh?.let { mesh ->
            // draw triangles
            mesh.render(pickShader, GL20.GL_TRIANGLES, 0, pickFaceIndexCount)
        }
        // -- edges (lines) --
        pickEdgeMesh?.let { mesh ->
            mesh.render(pickShader, GL20.GL_LINES, 0, pickEdgeIndexCount)
        }
        // -- vertices (points) --
        pickVertexMesh?.let { mesh ->
            // enable program point size for GL (desktop GL allows gl_PointSize in shader)
            Gdx.gl.glEnable(GL40.GL_PROGRAM_POINT_SIZE)
            mesh.render(pickShader, GL20.GL_POINTS, 0, pickVertexIndexCount)
            Gdx.gl.glDisable(GL40.GL_PROGRAM_POINT_SIZE)
        }

        // Read a single pixel at (x,y) from the FBO. Need a ByteBuffer of 4 bytes (RGBA).
        val px = x
        val py = y
        // Convert coordinate to FBO space: the FBO origin bottom-left, same as screen
        val readX = px
        val readY = py

        // Use glReadPixels to read pixel
        val pixel = BufferUtils.newByteBuffer(4)
        pixel.order(ByteOrder.nativeOrder())
        Gdx.gl.glReadPixels(readX, readY, 1, 1, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixel)

        // Unbind
        fbo.end()

        // Extract bytes
        pixel.rewind()
        val r = pixel.get().toInt() and 0xFF
        val g = pixel.get().toInt() and 0xFF
        val b = pixel.get().toInt() and 0xFF
        // val a = pixel.get().toInt() and 0xFF

        return PickEncoding.decodeFromBytes(r, g, b)
    }

    private fun ensurePickFbo(w: Int, h: Int) {
        if (pickFbo != null && w == pickFboWidth && h == pickFboHeight) return
        pickFbo?.dispose()
        pickFboWidth = max(1, w)
        pickFboHeight = max(1, h)
        pickFbo = FrameBuffer(Pixmap.Format.RGBA8888, pickFboWidth, pickFboHeight, true)
    }

    fun dispose() {
        visibleLineMesh?.dispose(); visibleLineMesh = null
        pickVertexMesh?.dispose(); pickVertexMesh = null
        pickEdgeMesh?.dispose(); pickEdgeMesh = null
        pickFaceMesh?.dispose(); pickFaceMesh = null
        visibleShader.dispose()
        pickShader.dispose()
        pickFbo?.dispose(); pickFbo = null
    }
}

class PickingTest : ApplicationAdapter() {
    lateinit var renderer: WireframePickerRenderer
    lateinit var mesh: Mesh
    lateinit var verticesList: List<Vertex>
    lateinit var edgesList: List<Edge>
    lateinit var facesList: List<Face>
    lateinit var camera: PerspectiveCamera
    private var showDebugPick: Boolean = false

    override fun create() {
        // Simple cube mesh (positions only)
        println("[DEBUG_LOG] PickingTest created. Press 'D' to toggle pick debug view.")
        mesh = loadFirstMeshFromGLTF("models/gltf/suzanne.gltf")

        // Build simple Indexed lists for cube
        verticesList = List(8) { i -> IVertex() }
        edgesList = List(12) { i -> IEdge() }
        facesList = List(12) { i -> IFace() }

        renderer = WireframePickerRenderer()
        renderer.buildFromMesh(mesh, verticesList, edgesList, facesList)

        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(1f,1f,1f)
        camera.lookAt(0f,0f,0f)
        camera.near = 0.1f
        camera.far = 100f
        camera.update()
    }

    override fun render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
//        renderer.renderVisible(camera.combined, Color.CYAN, lineWidth = 2f)

        if (Gdx.input.justTouched()) {
            val mx = Gdx.input.x
            val my = Gdx.input.y
            val pickResult = renderer.pickAt(mx, my, camera.combined)
            if (pickResult != null) {
                println("Picked: type=${pickResult.primitiveType}, index=${pickResult.primitiveIndex}")
                when (pickResult.primitiveType) {
                    PrimitiveType.Vertex -> {
                        val v = verticesList.getOrNull(pickResult.primitiveIndex)
                        println(" Vertex object: $v")
                    }

                    PrimitiveType.Edge -> {
                        val e = edgesList.getOrNull(pickResult.primitiveIndex)
                        println(" Edge object: $e")
                    }

                    PrimitiveType.Face -> {
                        val f = facesList.getOrNull(pickResult.primitiveIndex)
                        println(" Face object: $f")
                    }
                }
            } else {
                println("No pick")
            }
        }
        renderer.renderPickDebug(camera.combined)
    }

    override fun dispose() {
        renderer.dispose()
        mesh.dispose()
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }


}

fun main() {
    // This main function is just a placeholder.
    // To run the PickingTest, set it as the main application listener in your LibGDX project setup.
    Lwjgl3Application(PickingTest(), Lwjgl3ApplicationConfiguration().apply {
        setTitle("Wireframe Picker Test")
        setWindowedMode(800, 600)
        useVsync(true)
    })
}


