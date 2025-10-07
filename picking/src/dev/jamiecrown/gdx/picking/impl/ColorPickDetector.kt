package dev.jamiecrown.gdx.picking.impl

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.GdxRuntimeException
import dev.jamiecrown.gdx.picking.api.PickDetector
import dev.jamiecrown.gdx.picking.api.PixelDecoder
import kotlin.math.max
import org.lwjgl.opengl.GL40

/**
 * A reusable, generic color-picking detector. It renders encoded colors into an offscreen
 * framebuffer and decodes the pixel under the cursor using the supplied [PixelDecoder].
 *
 * The default mesh-building logic assigns unique ids to vertices, unique ids to unique edges
 * derived from triangle indices, and unique ids per triangle (face). These ids are encoded as
 * RGB using [ColorPickEncoding].
 */
class ColorPickDetector<T>(
    private val maxPickWidth: Int = 2048,
    private val maxPickHeight: Int = 2048,
    private val decoder: PixelDecoder<T>
) : PickDetector<T> {

    // pick meshes (pos + color)
    private var pickVertexMesh: Mesh? = null
    private var pickEdgeMesh: Mesh? = null
    private var pickFaceMesh: Mesh? = null
    private var pickVertexIndexCount = 0
    private var pickEdgeIndexCount = 0
    private var pickFaceIndexCount = 0

    private val pickShader: ShaderProgram

    private var pickFbo: FrameBuffer? = null
    private var pickFboWidth = 0
    private var pickFboHeight = 0

    init {
        ShaderProgram.pedantic = false
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
                gl_PointSize = 6.0;
              #endif
            }
            """.trimIndent(),
            // fragment: output color as-is
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

    fun dispose() {
        pickVertexMesh?.dispose(); pickVertexMesh = null
        pickEdgeMesh?.dispose(); pickEdgeMesh = null
        pickFaceMesh?.dispose(); pickFaceMesh = null
        pickShader.dispose()
        pickFbo?.dispose(); pickFbo = null
    }


    fun buildFromMesh(source: Mesh) {
        // --- read source arrays once ---
        val stride = source.vertexSize / 4
        val posAttr = source.getVertexAttribute(VertexAttributes.Usage.Position)
            ?: throw IllegalArgumentException("Mesh must have position attribute")
        val posOffset = posAttr.offset / 4
        val numVerts = source.numVertices
        val numIndices = source.numIndices

        val srcVerts = FloatArray(numVerts * stride)
        val srcIndices = ShortArray(numIndices)
        source.getVertices(srcVerts)
        source.getIndices(srcIndices)

        // --- build unique edge set from triangle list ---
        val edgeSet = LinkedHashSet<Pair<Int, Int>>()
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

        // 1) vertex pick mesh (GL_POINTS)
        val vertexPickPositions = FloatArray(numVerts * 3)
        val vertexPickColors = FloatArray(numVerts * 4)
        for (i in 0 until numVerts) {
            val posIdx = i * stride + posOffset
            vertexPickPositions[i * 3 + 0] = srcVerts[posIdx]
            vertexPickPositions[i * 3 + 1] = srcVerts[posIdx + 1]
            vertexPickPositions[i * 3 + 2] = srcVerts[posIdx + 2]
            val idColor = ColorPickEncoding.encodeToColorFloats(1, i)
            vertexPickColors[i * 4 + 0] = idColor[0]
            vertexPickColors[i * 4 + 1] = idColor[1]
            vertexPickColors[i * 4 + 2] = idColor[2]
            vertexPickColors[i * 4 + 3] = 1f
        }
        val vertexPickIndices = ShortArray(numVerts) { it.toShort() }

        pickVertexMesh?.dispose()
        pickVertexMesh = Mesh(true, numVerts, numVerts,
            VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color")
        )
        val vertexInterleaved = FloatArray(numVerts * 7)
        for (i in 0 until numVerts) {
            val srcBase = i * 3
            val dstBase = i * 7
            vertexInterleaved[dstBase + 0] = vertexPickPositions[srcBase + 0]
            vertexInterleaved[dstBase + 1] = vertexPickPositions[srcBase + 1]
            vertexInterleaved[dstBase + 2] = vertexPickPositions[srcBase + 2]
            vertexInterleaved[dstBase + 3] = vertexPickColors[srcBase + 0]
            vertexInterleaved[dstBase + 4] = vertexPickColors[srcBase + 1]
            vertexInterleaved[dstBase + 5] = vertexPickColors[srcBase + 2]
            vertexInterleaved[dstBase + 6] = vertexPickColors[srcBase + 3]
        }
        pickVertexMesh!!.setVertices(vertexInterleaved)
        pickVertexMesh!!.setIndices(vertexPickIndices)
        pickVertexIndexCount = vertexPickIndices.size

        // 2) edge pick mesh (GL_LINES)
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
            val colorA = ColorPickEncoding.encodeToColorFloats(2, edgeIdx)
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
            val colorB = ColorPickEncoding.encodeToColorFloats(2, edgeIdx)
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
        val edgeInterleaved = FloatArray(edgePickPositions.size / 3 * 7)
        val edgeVertexCount = edgePickPositions.size / 3
        for (i in 0 until edgeVertexCount) {
            val p = i * 3
            val d = i * 7
            edgeInterleaved[d + 0] = edgePickPositions[p + 0]
            edgeInterleaved[d + 1] = edgePickPositions[p + 1]
            edgeInterleaved[d + 2] = edgePickPositions[p + 2]
            edgeInterleaved[d + 3] = edgePickColors[p + 0]
            edgeInterleaved[d + 4] = edgePickColors[p + 1]
            edgeInterleaved[d + 5] = edgePickColors[p + 2]
            edgeInterleaved[d + 6] = edgePickColors[p + 3]
        }
        pickEdgeMesh!!.setVertices(edgeInterleaved)
        pickEdgeMesh!!.setIndices(edgePickIndices)
        pickEdgeIndexCount = edgePickIndices.size

        // 3) face pick mesh (GL_TRIANGLES)
        val triCount = triangleCount
        val facePickPositions = FloatArray(triCount * 3 * 3)
        val facePickColors = FloatArray(triCount * 3 * 4)
        val facePickIndices = ShortArray(triCount * 3)
        var fiVert = 0
        for (t in 0 until triCount) {
            val aIdx = srcIndices[t * 3].toInt() and 0xFFFF
            val bIdx = srcIndices[t * 3 + 1].toInt() and 0xFFFF
            val cIdx = srcIndices[t * 3 + 2].toInt() and 0xFFFF
            val positions = intArrayOf(aIdx, bIdx, cIdx)
            val colorFloats = ColorPickEncoding.encodeToColorFloats(3, t)
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
        val faceInterleaved = FloatArray(facePickPositions.size / 3 * 7)
        val faceVertexCount = facePickPositions.size / 3
        for (i in 0 until faceVertexCount) {
            val p = i * 3
            val d = i * 7
            faceInterleaved[d + 0] = facePickPositions[p + 0]
            faceInterleaved[d + 1] = facePickPositions[p + 1]
            faceInterleaved[d + 2] = facePickPositions[p + 2]
            faceInterleaved[d + 3] = facePickColors[p + 0]
            faceInterleaved[d + 4] = facePickColors[p + 1]
            faceInterleaved[d + 5] = facePickColors[p + 2]
            faceInterleaved[d + 6] = facePickColors[p + 3]
        }
        pickFaceMesh!!.setVertices(faceInterleaved)
        pickFaceMesh!!.setIndices(facePickIndices)
        pickFaceIndexCount = facePickIndices.size
    }

    override fun pickAt(x: Int, y: Int, cameraCombined: Matrix4, pickWidth: Int, pickHeight: Int): T? {
        ensurePickFbo(
            w = minOf(if (pickWidth <= 0) max(1, Gdx.graphics.width) else pickWidth, maxPickWidth),
            h = minOf(if (pickHeight <= 0) max(1, Gdx.graphics.height) else pickHeight, maxPickHeight)
        )
        val fbo = pickFbo ?: return null

        // Bind FBO and render pass
        fbo.begin()
        // Clear to zero (no hit)
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        pickShader.bind()
        pickShader.setUniformMatrix("u_projModelView", cameraCombined)

        // faces first
        pickFaceMesh?.let { it.render(pickShader, GL20.GL_TRIANGLES, 0, pickFaceIndexCount) }
        // edges
        pickEdgeMesh?.let { it.render(pickShader, GL20.GL_LINES, 0, pickEdgeIndexCount) }
        // vertices
        pickVertexMesh?.let {
            Gdx.gl.glEnable(GL40.GL_PROGRAM_POINT_SIZE)
            it.render(pickShader, GL20.GL_POINTS, 0, pickVertexIndexCount)
            Gdx.gl.glDisable(GL40.GL_PROGRAM_POINT_SIZE)
        }

        val readX = x
        val readY = y
        val pixel = BufferUtils.newByteBuffer(4)
        Gdx.gl.glReadPixels(readX, readY, 1, 1, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixel)
        fbo.end()

        pixel.rewind()
        val r = pixel.get().toInt() and 0xFF
        val g = pixel.get().toInt() and 0xFF
        val b = pixel.get().toInt() and 0xFF
        val a = pixel.get().toInt() and 0xFF
        return decoder.decode(r, g, b, a)
    }

    private fun ensurePickFbo(w: Int, h: Int) {
        if (pickFbo != null && w == pickFboWidth && h == pickFboHeight) return
        pickFbo?.dispose()
        pickFboWidth = kotlin.math.max(1, w)
        pickFboHeight = kotlin.math.max(1, h)
        pickFbo = FrameBuffer(Pixmap.Format.RGBA8888, pickFboWidth, pickFboHeight, true)
    }
}
