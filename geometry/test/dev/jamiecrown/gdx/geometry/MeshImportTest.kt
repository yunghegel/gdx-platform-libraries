package dev.jamiecrown.gdx.geometry

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttributes
import dev.jamiecrown.gdx.geometry.model.bm.BMesh
import dev.jamiecrown.gdx.geometry.model.bm.struct.BMVertex
import dev.jamiecrown.gdx.geometry.model.he.HalfEdgeMesh
import dev.jamiecrown.gdx.geometry.model.ifs.IFSMesh
import dev.jamiecrown.gdx.test.LibGdxConfig
import dev.jamiecrown.gdx.test.LibGdxTestContext
import dev.jamiecrown.gdx.test.LibGdxTestExtension
import net.mgsx.gltf.loaders.gltf.GLTFLoader
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(LibGdxTestExtension::class)
@LibGdxConfig(mode = LibGdxConfig.Mode.WINDOWED, width = 320, height = 240, frames = 1, title = "Mesh Import Test")
class MeshImportTest {

    private fun loadFirstMeshFromGLTF(path: String): Mesh {
        val sceneAsset = GLTFLoader().load(Gdx.files.internal(path))
        // In Scene3D, SceneAsset.scene is a ModelInstance. Get its Model, then first Mesh.
        val model = sceneAsset.scene.model
        require(model.meshes.size > 0) { "No meshes found in GLTF at $path" }
        return model.meshes.first()
    }

    @Test
    @LibGdxConfig(mode = LibGdxConfig.Mode.HEADLESS)
    fun import_into_IFS_HE_BM(context: LibGdxTestContext) {
        context.create {
            val mesh = loadFirstMeshFromGLTF("models/gltf/logo.gltf")

            // IFSMesh: uses built-in loader for vertex attributes
            val ifs = IFSMesh()
            ifs.loadFromMesh(mesh)
            assertTrue(ifs.vertexData.size > 0, "IFSMesh should have vertices after load")

            // HalfEdgeMesh: has an importer that creates vertices/faces/half-edges
            val he = HalfEdgeMesh()
            he.importFromMesh(mesh)
            assertTrue(he.vertexData.size > 0, "HalfEdgeMesh should have vertices after import")
            assertTrue(he.faceData.size > 0, "HalfEdgeMesh should have faces after import")

            // BMesh: populate manually using mesh vertices and indices
            val bm = BMesh()
            bm.initializeMesh(mesh)

            // Read positions from Mesh
            val vsize = mesh.vertexSize / 4
            val vcount = mesh.numVertices
            val vdata = FloatArray(vcount * vsize)
            mesh.getVertices(vdata)

            // Find position attribute offset
            val attrs: VertexAttributes = mesh.vertexAttributes
            var posOffset = -1
            run {
                for (i in 0 until attrs.size()) {
                    val a = attrs.get(i)
                    if (a.usage == VertexAttributes.Usage.Position) {
                        posOffset = a.offset / 4
                        break
                    }
                }
            }
            require(posOffset >= 0) { "Mesh missing position attribute" }

            val bmVerts = ArrayList<BMVertex>(vcount)
            for (i in 0 until vcount) {
                val base = i * vsize + posOffset
                val vx = vdata[base]
                val vy = vdata[base + 1]
                val vz = vdata[base + 2]
                bmVerts.add(bm.createVertex(vx, vy, vz))
            }

            // Triangles
            val icount = mesh.numIndices
            val idata = ShortArray(icount)
            mesh.getIndices(idata)
            for (i in 0 until icount step 3) {
                val i0 = idata[i].toInt()
                val i1 = idata[i + 1].toInt()
                val i2 = idata[i + 2].toInt()
                bm.createFace(bmVerts[i0], bmVerts[i1], bmVerts[i2])
            }

            assertTrue(bm.vertexData.size > 0, "BMesh should have vertices after populate")
            assertTrue(bm.faceData.size > 0, "BMesh should have faces after populate")
        }
        // Start the LibGDX application and run a single frame to execute the create{} block
        context.run(framesOverride = 1)
    }
}
