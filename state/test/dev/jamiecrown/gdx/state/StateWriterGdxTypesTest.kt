package dev.jamiecrown.gdx.state

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.*
import dev.jamiecrown.gdx.state.app.AppStateManager
import dev.jamiecrown.gdx.state.app.AppStateNode
import dev.jamiecrown.gdx.state.app.StateReader
import dev.jamiecrown.gdx.state.app.StateWriter
import dev.jamiecrown.gdx.state.storage.FileKeyValueStore
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StateWriterGdxTypesTest {

    private class GdxTypesNode : AppStateNode {
        override val id: String = "GdxTypes"
        var v2: Vector2 = Vector2(1.25f, -2.5f)
        var v3: Vector3 = Vector3(0.1f, 0.2f, 0.3f)
        var v4: Vector4 = Vector4(9.9f, -8.8f, 7.7f, -6.6f)
        var c: Color = Color(0.4f, 0.5f, 0.6f, 0.7f)
        var q: Quaternion = Quaternion(0.11f, 0.22f, 0.33f, 0.44f)
        var m3: Matrix3 = Matrix3().also { it.`val`[0] = 1.1f; it.`val`[4] = 2.2f; it.`val`[8] = 3.3f }
        var m4: Matrix4 = Matrix4().also { it.`val`[0] = 4.4f; it.`val`[5] = 5.5f; it.`val`[10] = 6.6f; it.`val`[15] = 1f }

        override fun writeState(writer: StateWriter) {
            println("[DEBUG_LOG] GdxTypesNode.writeState: v2=$v2 v3=$v3 v4=$v4 c=$c q=$q m3=${m3.`val`.toList()} m4=${m4.`val`.toList()}")
            writer.put("v2", v2)
            writer.put("v3", v3)
            writer.put("v4", v4)
            writer.put("c", c)
            writer.put("q", q)
            writer.put("m3", m3)
            writer.put("m4", m4)
        }

        override fun readState(reader: StateReader) {
            v2 = reader.getVector2("v2")
            v3 = reader.getVector3("v3")
            v4 = reader.getVector4("v4")
            c = reader.getColor("c")
            q = reader.getQuaternion("q")
            m3 = reader.getMatrix3("m3")
            m4 = reader.getMatrix4("m4")
            println("[DEBUG_LOG] GdxTypesNode.readState: v2=$v2 v3=$v3 v4=$v4 c=$c q=$q m3=${m3.`val`.toList()} m4=${m4.`val`.toList()}")
        }
    }

    @Test
    fun roundTripLibGdxTypes() {
        println("[DEBUG_LOG] Starting StateWriterGdxTypesTest.roundTripLibGdxTypes")
        val dir = java.io.File("state/testdata/StateWriterGdxTypesTest-" + System.currentTimeMillis()).apply { mkdirs() }
        val store = FileKeyValueStore(dir, "app")
        val node = GdxTypesNode()
        val mgr = AppStateManager(store).register(node)
        mgr.saveAll()

        // Assert pretty, YAML-like tagged format persisted
        val persisted = store.read("GdxTypes")!!
        println("[DEBUG_LOG] Persisted map: ${'$'}persisted")
        val v2s = persisted["v2"]!!
        val v3s = persisted["v3"]!!
        val v4s = persisted["v4"]!!
        val cs = persisted["c"]!!
        val qs = persisted["q"]!!
        val m3s = persisted["m3"]!!
        val m4s = persisted["m4"]!!
        assertTrue(v2s.startsWith("!vec2 ["), "v2 not tagged properly: ${'$'}v2s")
        assertTrue(v3s.startsWith("!vec3 ["), "v3 not tagged properly: ${'$'}v3s")
        assertTrue(v4s.startsWith("!vec4 ["), "v4 not tagged properly: ${'$'}v4s")
        assertTrue(cs.startsWith("!color ["), "c not tagged properly: ${'$'}cs")
        assertTrue(qs.startsWith("!quat ["), "q not tagged properly: ${'$'}qs")
        assertTrue(m3s.startsWith("!mat3 ["), "m3 not tagged properly: ${'$'}m3s")
        assertTrue(m4s.startsWith("!mat4 ["), "m4 not tagged properly: ${'$'}m4s")

        // mutate values to ensure load actually restores
        node.v2 = Vector2.Zero.cpy()
        node.v3 = Vector3.Zero.cpy()
        node.v4 = Vector4.Zero.cpy()
        node.c = Color(1f,1f,1f,1f)
        node.q = Quaternion()
        node.m3 = Matrix3()
        node.m4 = Matrix4()
        println("[DEBUG_LOG] After mutation: v2=${'$'}{node.v2} v3=${'$'}{node.v3} v4=${'$'}{node.v4} c=${'$'}{node.c} q=${'$'}{node.q} m3=${'$'}{node.m3.`val`.toList()} m4=${'$'}{node.m4.`val`.toList()}")

        mgr.loadAll()

        // Validate with tolerant comparisons where necessary
        assertEquals(1.25f, node.v2.x, 1e-6f)
        assertEquals(-2.5f, node.v2.y, 1e-6f)

        assertEquals(0.1f, node.v3.x, 1e-6f)
        assertEquals(0.2f, node.v3.y, 1e-6f)
        assertEquals(0.3f, node.v3.z, 1e-6f)

        assertEquals(9.9f, node.v4.x, 1e-6f)
        assertEquals(-8.8f, node.v4.y, 1e-6f)
        assertEquals(7.7f, node.v4.z, 1e-6f)
        assertEquals(-6.6f, node.v4.w, 1e-6f)

        assertEquals(0.4f, node.c.r, 1e-6f)
        assertEquals(0.5f, node.c.g, 1e-6f)
        assertEquals(0.6f, node.c.b, 1e-6f)
        assertEquals(0.7f, node.c.a, 1e-6f)

        assertEquals(0.11f, node.q.x, 1e-6f)
        assertEquals(0.22f, node.q.y, 1e-6f)
        assertEquals(0.33f, node.q.z, 1e-6f)
        assertEquals(0.44f, node.q.w, 1e-6f)

        val expectedM3 = FloatArray(9) { 0f }.also { it[0]=1.1f; it[4]=2.2f; it[8]=3.3f }
        val expectedM4 = FloatArray(16) { 0f }.also { it[0]=4.4f; it[5]=5.5f; it[10]=6.6f; it[15]=1f }
        println("[DEBUG_LOG] Validating matrices: expected m3=${'$'}{expectedM3.toList()} loaded m3=${'$'}{node.m3.`val`.toList()}\nexpected m4=${'$'}{expectedM4.toList()} loaded m4=${'$'}{node.m4.`val`.toList()}")
        assertArrayEquals(expectedM3, node.m3.`val`, 1e-6f)
        assertArrayEquals(expectedM4, node.m4.`val`, 1e-6f)

        println("[DEBUG_LOG] Completed StateWriterGdxTypesTest.roundTripLibGdxTypes (files at ${'$'}dir)")
    }

    @Test
    fun loadLegacyCommaSeparatedFormat() {
        println("[DEBUG_LOG] Starting StateWriterGdxTypesTest.loadLegacyCommaSeparatedFormat")
        val dir = java.io.File("state/testdata/StateWriterGdxLegacy-" + System.currentTimeMillis()).apply { mkdirs() }
        val nsDir = java.io.File(dir, "app").apply { mkdirs() }
        val propsFile = java.io.File(nsDir, "GdxTypes.properties")
        val p = java.util.Properties()
        p.setProperty("v2", "1.25,-2.5")
        p.setProperty("v3", "0.1,0.2,0.3")
        p.setProperty("v4", "9.9,-8.8,7.7,-6.6")
        p.setProperty("c", "0.4,0.5,0.6,0.7")
        p.setProperty("q", "0.11,0.22,0.33,0.44")
        p.setProperty("m3", "1.1,0.0,0.0,0.0,2.2,0.0,0.0,0.0,3.3")
        p.setProperty("m4", "4.4,0.0,0.0,0.0,0.0,5.5,0.0,0.0,0.0,0.0,6.6,0.0,0.0,0.0,0.0,1.0")
        java.io.FileOutputStream(propsFile).use { out -> p.store(out, "App state for 'GdxTypes' (legacy)") }
        println("[DEBUG_LOG] Wrote legacy properties file at ${'$'}propsFile")

        val store = FileKeyValueStore(dir, "app")
        val node = GdxTypesNode().apply {
            // Overwrite initial to ensure loading occurs
            v2 = Vector2.Zero.cpy(); v3 = Vector3.Zero.cpy(); v4 = Vector4.Zero.cpy();
            c = Color(1f,1f,1f,1f); q = Quaternion(); m3 = Matrix3(); m4 = Matrix4()
        }
        val mgr = AppStateManager(store).register(node)
        mgr.loadAll()

        println("[DEBUG_LOG] Loaded from legacy. v2=${'$'}{node.v2} v3=${'$'}{node.v3} v4=${'$'}{node.v4} c=${'$'}{node.c} q=${'$'}{node.q}")
        // Validate key values
        assertEquals(1.25f, node.v2.x, 1e-6f)
        assertEquals(-2.5f, node.v2.y, 1e-6f)
        assertEquals(0.11f, node.q.x, 1e-6f)
        assertEquals(0.44f, node.q.w, 1e-6f)
        println("[DEBUG_LOG] Completed StateWriterGdxTypesTest.loadLegacyCommaSeparatedFormat")
    }
}
