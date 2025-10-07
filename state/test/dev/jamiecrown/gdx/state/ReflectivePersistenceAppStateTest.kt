package dev.jamiecrown.gdx.state

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import dev.jamiecrown.gdx.state.app.AppStateManager
import dev.jamiecrown.gdx.state.app.AppStateNode
import dev.jamiecrown.gdx.state.storage.FileKeyValueStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReflectivePersistenceAppStateTest {

    enum class Mode { A, B, C }

    private class Node : AppStateNode {
        override val id: String = "ReflectiveNode"
        @Persist var name: String = ""
        @Persist var count: Int = 0
        @Persist var enabled: Boolean = false
        @Persist var mode: Mode = Mode.A
        @Persist var tint: Color = Color(0.1f, 0.2f, 0.3f, 0.4f)
        @Persist var position: Vector3 = Vector3(1f, 2f, 3f)
        @Persist var flags: Map<String, Boolean> = linkedMapOf("a" to true, "b" to false)
        @Persist var weights: Map<String, Int> = linkedMapOf("x" to 1, "y" to -2)
        @Persist var labels: Map<String, String> = linkedMapOf("k" to "v", "needs space" to "a b")
    }

    @Test
    fun testReflectiveRoundTrip() {
        println("[DEBUG_LOG] Starting ReflectivePersistenceAppStateTest.testReflectiveRoundTrip")
        val dir = java.io.File("state/testdata/ReflectivePersistenceAppStateTest-" + System.currentTimeMillis()).apply { mkdirs() }
        val store = FileKeyValueStore(dir, "app")
        val node = Node().apply {
            name = "hello"
            count = 42
            enabled = true
            mode = Mode.C
            tint = Color(0.9f, 0.8f, 0.7f, 0.6f)
            position.set(9f, 8f, 7f)
            flags = linkedMapOf("a" to false, "b" to true)
            weights = linkedMapOf("x" to 5, "y" to -6)
            labels = linkedMapOf("plain" to "value", "quote\"key" to "quote\"value")
        }
        val mgr = AppStateManager(store).register(node)
        mgr.saveAll()
        // mutate to verify reload
        node.name = ""
        node.count = 0
        node.enabled = false
        node.mode = Mode.A
        node.tint = Color(0.1f, 0.2f, 0.3f, 0.4f)
        node.position.set(0f, 0f, 0f)
        node.flags = emptyMap()
        node.weights = emptyMap()
        node.labels = emptyMap()

        mgr.loadAll()

        assertEquals("hello", node.name)
        assertEquals(42, node.count)
        assertEquals(true, node.enabled)
        assertEquals(Mode.C, node.mode)
        assertEquals(Color(0.9f, 0.8f, 0.7f, 0.6f), node.tint)
        assertEquals(Vector3(9f, 8f, 7f), node.position)
        assertEquals(linkedMapOf("a" to false, "b" to true), node.flags)
        assertEquals(linkedMapOf("x" to 5, "y" to -6), node.weights)
        assertEquals(linkedMapOf("plain" to "value", "quote\"key" to "quote\"value"), node.labels)
        println("[DEBUG_LOG] Completed ReflectivePersistenceAppStateTest.testReflectiveRoundTrip (files at $dir)")
    }
}