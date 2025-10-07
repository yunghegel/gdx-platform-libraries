package dev.jamiecrown.gdx.state

import dev.jamiecrown.gdx.state.app.AppStateManager
import dev.jamiecrown.gdx.state.app.AppStateNode
import dev.jamiecrown.gdx.state.app.StateReader
import dev.jamiecrown.gdx.state.app.StateWriter
import dev.jamiecrown.gdx.state.storage.FileKeyValueStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StateMapPrimitivesTest {

    private class MapNode : AppStateNode {
        override val id: String = "MapNode"
        var ints: Map<String, Int> = linkedMapOf()
        var floats: Map<String, Float> = linkedMapOf()
        var bools: Map<String, Boolean> = linkedMapOf()
        var strings: Map<String, String> = linkedMapOf()
        var singleString: String = ""
        override fun writeState(writer: StateWriter) {
            println("[DEBUG_LOG] MapNode.writeState ints=$ints floats=$floats bools=$bools strings=$strings singleString='$singleString'")
            writer.putMapInt("ints", ints)
            writer.putMapFloat("floats", floats)
            writer.putMapBoolean("bools", bools)
            writer.putMapString("strings", strings)
            writer.put("singleString", singleString)
        }
        override fun readState(reader: StateReader) {
            ints = reader.getMapInt("ints")
            floats = reader.getMapFloat("floats")
            bools = reader.getMapBoolean("bools")
            strings = reader.getMapString("strings")
            singleString = reader.get("singleString", "") ?: ""
            println("[DEBUG_LOG] MapNode.readState ints=$ints floats=$floats bools=$bools strings=$strings singleString='$singleString'")
        }
    }

    @Test
    fun testMapPrimitivesRoundTrip() {
        println("[DEBUG_LOG] Starting StateMapPrimitivesTest.testMapPrimitivesRoundTrip")
        val dir = java.io.File("state/testdata/StateMapPrimitivesTest-" + System.currentTimeMillis()).apply { mkdirs() }
        val store = FileKeyValueStore(dir, "app")
        val node = MapNode().apply {
            ints = linkedMapOf("a" to 1, "b" to 2)
            floats = linkedMapOf("x" to 1.5f, "y" to -2.25f)
            bools = linkedMapOf("on" to true, "off" to false)
            singleString = "hello world"
        }
        val mgr = AppStateManager(store).register(node)
        mgr.saveAll()
        // mutate
        node.ints = emptyMap()
        node.floats = emptyMap()
        node.bools = emptyMap()
        node.singleString = ""
        mgr.loadAll()
        assertEquals(linkedMapOf("a" to 1, "b" to 2), node.ints)
        assertEquals(linkedMapOf("x" to 1.5f, "y" to -2.25f), node.floats)
        assertEquals(linkedMapOf("on" to true, "off" to false), node.bools)
        assertEquals("hello world", node.singleString)
        println("[DEBUG_LOG] Completed StateMapPrimitivesTest.testMapPrimitivesRoundTrip (files at $dir)")
    }

    @Test
    fun testMapStringRoundTrip() {
        println("[DEBUG_LOG] Starting StateMapPrimitivesTest.testMapStringRoundTrip")
        val dir = java.io.File("state/testdata/StateMapPrimitivesTest-" + System.currentTimeMillis()).apply { mkdirs() }
        val store = FileKeyValueStore(dir, "app")
        val node = MapNode().apply {
            strings = linkedMapOf(
                "plain" to "value",
                "needs space" to "has space",
                "quote\"key" to "quote\"value",
                "slash" to "a \\ slash"
            )
        }
        val mgr = AppStateManager(store).register(node)
        mgr.saveAll()
        // mutate
        node.strings = emptyMap()
        mgr.loadAll()
        assertEquals(
            linkedMapOf(
                "plain" to "value",
                "needs space" to "has space",
                "quote\"key" to "quote\"value",
                "slash" to "a \\ slash"
            ),
            node.strings
        )
        println("[DEBUG_LOG] Completed StateMapPrimitivesTest.testMapStringRoundTrip (files at $dir)")
    }
}
