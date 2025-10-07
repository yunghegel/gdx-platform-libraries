package dev.jamiecrown.gdx.state

import dev.jamiecrown.gdx.state.app.AppStateManager
import dev.jamiecrown.gdx.state.app.AppStateNode
import dev.jamiecrown.gdx.state.app.StateReader
import dev.jamiecrown.gdx.state.app.StateWriter
import dev.jamiecrown.gdx.state.storage.FileKeyValueStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StateFileFormatTest {

    private class StringsNode : AppStateNode {
        override val id: String = "Stringy"
        var labels: Map<String, String> = linkedMapOf(
            "plain" to "value",
            "needs space" to "a b",
            "quote\"key" to "quote\"value",
            "path" to "C:\\temp\\file.txt",
            "json" to "{\"a\":1, \"b\": [1,2,3]}"
        )
        override fun writeState(writer: StateWriter) {
            println("[DEBUG_LOG] StringsNode.writeState: labels=$labels")
            writer.putMapString("labels", labels)
        }
        override fun readState(reader: StateReader) {
            labels = reader.getMapString("labels")
            println("[DEBUG_LOG] StringsNode.readState: labels=$labels")
        }
    }

    @Test
    fun writesHoconLikeWithoutEscapes() {
        println("[DEBUG_LOG] Starting StateFileFormatTest.writesHoconLikeWithoutEscapes")
        val dir = java.io.File("state/testdata/StateFileFormatTest-" + System.currentTimeMillis()).apply { mkdirs() }
        val store = FileKeyValueStore(dir, "app")
        val node = StringsNode()
        val mgr = AppStateManager(store).register(node)
        mgr.saveAll()

        val file = java.io.File(dir, "app/Stringy.properties")
        assertTrue(file.exists(), "Expected file to be written: $file")
        val text = file.readText()
        println("[DEBUG_LOG] Saved file contents:\n$text")

        // Should not contain Java-Properties style escaping (e.g., backslashes before spaces/colons)
        assertFalse(text.contains("\\:"), "File contains properties-style escaped colon")
        assertFalse(text.contains("\\="), "File contains properties-style escaped equals")

        // Our mapStr uses single-quoted values; ensure we didn't inject backslash escapes for quotes
        assertTrue(text.contains("labels = !mapStr {"), "Expected mapStr tag present")
        assertTrue(text.contains("'needs space': 'a b'"), "Expected single-quoted key and value without escapes")
        assertTrue(text.contains("'quote\"key': 'quote\"value'"), "Double quotes should not be escaped inside single quotes")
        assertTrue(text.contains("path: 'C:"), "Expected path key with single-quoted Windows path literal present")

        // Now verify we can load it back
        node.labels = emptyMap()
        mgr.loadAll()
        println("[DEBUG_LOG] Reloaded labels=${'$'}{node.labels}")
        assertEquals("value", node.labels["plain"])
        assertEquals("a b", node.labels["needs space"])
        assertEquals("quote\"value", node.labels["quote\"key"])
        assertEquals("C:\\temp\\file.txt", node.labels["path"])
        assertEquals("{\"a\":1, \"b\": [1,2,3]}", node.labels["json"])

        println("[DEBUG_LOG] Completed StateFileFormatTest.writesHoconLikeWithoutEscapes (file at ${'$'}file)")
    }
}
