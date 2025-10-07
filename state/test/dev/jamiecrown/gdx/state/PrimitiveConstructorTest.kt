package dev.jamiecrown.gdx.state

import dev.jamiecrown.gdx.state.app.AppStateManager
import dev.jamiecrown.gdx.state.storage.FileKeyValueStore
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrimitiveConstructorTest {

    private fun tempDir(): File {
        val dir = File("testdata/PrimitiveConstructorTest-${System.nanoTime()}")
        println("[DEBUG_LOG] Creating temp dir for PrimitiveConstructorTest: ${dir.absolutePath}")
        dir.mkdirs()
        return dir
    }

    class NeedsPrimitives(val x: Int, val y: Boolean) {
        override fun toString(): String = "NeedsPrimitives(x=$x, y=$y)"
    }

    @Test
    fun testNewWithPrimitiveArgs() {
        println("[DEBUG_LOG] testNewWithPrimitiveArgs START")
        val state = AppStateManager(FileKeyValueStore(tempDir()))
        Injector.configure(state)

        val inst: NeedsPrimitives by new(3, true)
        val obj = inst // trigger creation
        println("[DEBUG_LOG] Created instance: $obj")
        assertEquals(3, obj.x, "[DEBUG_LOG] x should be 3")
        assertTrue(obj.y, "[DEBUG_LOG] y should be true")
        println("[DEBUG_LOG] testNewWithPrimitiveArgs END")
    }

    @Test
    fun testProvisionAndInjectWithPrimitiveArgs() {
        println("[DEBUG_LOG] testProvisionAndInjectWithPrimitiveArgs START")
        val state = AppStateManager(FileKeyValueStore(tempDir()))
        Injector.configure(state)

        val provided: NeedsPrimitives by provision(7, false)
        val p = provided
        println("[DEBUG_LOG] Provisioned: $p")
        val injected: NeedsPrimitives by inject()
        val q = injected
        println("[DEBUG_LOG] Injected: $q")
        assertTrue(p === q, "[DEBUG_LOG] inject() should return the same singleton instance as provision()")
        assertEquals(7, q.x, "[DEBUG_LOG] x should be 7")
        assertEquals(false, q.y, "[DEBUG_LOG] y should be false")
        println("[DEBUG_LOG] testProvisionAndInjectWithPrimitiveArgs END")
    }
}
