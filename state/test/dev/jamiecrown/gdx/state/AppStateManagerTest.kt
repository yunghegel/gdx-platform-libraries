package dev.jamiecrown.gdx.state

import dev.jamiecrown.gdx.state.app.AppStateManager
import dev.jamiecrown.gdx.state.app.AppStateNode
import dev.jamiecrown.gdx.state.app.StateReader
import dev.jamiecrown.gdx.state.app.StateWriter
import dev.jamiecrown.gdx.state.storage.FileKeyValueStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files

class AppStateManagerTest {

    class NodeA : AppStateNode {
        override val id = "A"
        var value: String = ""
        override fun writeState(writer: StateWriter) {
            println("[DEBUG_LOG] NodeA.writeState value='${'$'}value'")
            writer.put("value", value)
        }
        override fun readState(reader: StateReader) {
            value = reader.get("value", "") ?: ""
            println("[DEBUG_LOG] NodeA.readState value='${'$'}value'")
        }
    }

    class NodeB(private val a: NodeA) : AppStateNode {
        override val id = "B"
        override val dependsOn = setOf("A")
        var number: Int = 0
        override fun writeState(writer: StateWriter) {
            println("[DEBUG_LOG] NodeB.writeState number=${'$'}number (depends on A='${'$'}{a.value}')")
            writer.putInt("number", number)
        }
        override fun readState(reader: StateReader) {
            number = reader.getInt("number", -1)
            println("[DEBUG_LOG] NodeB.readState number=${'$'}number with A='${'$'}{a.value}'")
        }
    }

    class NodeC(private val b: NodeB) : AppStateNode {
        override val id = "C"
        override val dependsOn = setOf("B")
        var flag: Boolean = false
        override fun writeState(writer: StateWriter) {
            println("[DEBUG_LOG] NodeC.writeState flag=${'$'}flag (depends on B=${'$'}{b.number})")
            writer.putBoolean("flag", flag)
        }
        override fun readState(reader: StateReader) {
            flag = reader.getBoolean("flag", true)
            println("[DEBUG_LOG] NodeC.readState flag=${'$'}flag with B=${'$'}{b.number}")
        }
    }

    @Test
    fun testSaveLoadOrderAndValues() {
        println("[DEBUG_LOG] Starting AppStateManagerTest.testSaveLoadOrderAndValues")
        val dir = java.io.File("testdata/AppStateManagerTest-" + System.currentTimeMillis()).apply { mkdirs() }
        val store = FileKeyValueStore(dir, "app")
        val a = NodeA().apply { value = "hello" }
        val b = NodeB(a).apply { number = 42 }
        val c = NodeC(b).apply { flag = true }
        val manager = AppStateManager(store)
            .register(c) // register out of order intentionally
            .register(a)
            .register(b)
        manager.saveAll()
        // mutate to ensure values are actually loaded
        a.value = ""
        b.number = 0
        c.flag = false
        manager.loadAll()
        assertEquals("hello", a.value)
        assertEquals(42, b.number)
        assertEquals(true, c.flag)
        println("[DEBUG_LOG] Completed AppStateManagerTest.testSaveLoadOrderAndValues (files at ${'$'}dir)")
    }
}
