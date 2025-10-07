package dev.jamiecrown.gdx.state

import dev.jamiecrown.gdx.state.app.AppStateManager
import dev.jamiecrown.gdx.state.eventbus.EventBus
import dev.jamiecrown.gdx.state.eventbus.Subscribe
import dev.jamiecrown.gdx.state.storage.FileKeyValueStore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class InjectorTest {

    private fun tempDir(): File {
        val dir = File("testdata/InjectorTest-${System.currentTimeMillis()}")
        println("[DEBUG_LOG] Creating non-temp dir for InjectorTest: ${dir.absolutePath}")
        dir.mkdirs()
        return dir
    }

    data class Ping(val msg: String)

    @StateId("TestListenerState")
    class TestListener {
        @Persist("counter")
        var counter: Int = 0

        @Persist("label")
        var label: String = "unset"

        var lastMsg: String? = null

        @Subscribe
        fun onPing(p: Ping) {
            println("[DEBUG_LOG] TestListener received Ping('${p.msg}')")
            lastMsg = p.msg
        }
    }

    @Test
    fun testEventRegistrationAndPersistence() {
        println("[DEBUG_LOG] testEventRegistrationAndPersistence START")
        val storeDir = tempDir()
        val state = AppStateManager(FileKeyValueStore(storeDir))
        Injector.configure(state)

        // Create via delegate
        val listener: TestListener by new()
        val l = listener // trigger creation
        println("[DEBUG_LOG] Created listener with initial counter=${l.counter} label='${l.label}'")

        // Event bus delivery
        EventBus.post(Ping("hello"))
        assertEquals("hello", l.lastMsg, "[DEBUG_LOG] Expected lastMsg to be 'hello' after EventBus.post")

        // Modify and persist
        l.counter = 42
        l.label = "alpha"
        Injector.saveAll()
        println("[DEBUG_LOG] Saved state. Now recreating instance to verify reload.")

        // Recreate new instance; state should be loaded
        val listener2: TestListener by new()
        val l2 = listener2
        println("[DEBUG_LOG] Recreated listener with counter=${l2.counter} label='${l2.label}'")
        assertEquals(42, l2.counter, "[DEBUG_LOG] Counter should have been restored from persisted state")
        assertEquals("alpha", l2.label, "[DEBUG_LOG] Label should have been restored from persisted state")
        println("[DEBUG_LOG] testEventRegistrationAndPersistence END")
    }

    class CtorMatch(a: Number, b: String) {
        constructor(a: Int): this(a, "default")
        var seenA: Number = a
        var seenB: String = b
    }

    @Test
    fun testConstructorSelectionAndProvision() {
        println("[DEBUG_LOG] testConstructorSelectionAndProvision START")
        val state = AppStateManager(FileKeyValueStore(tempDir()))
        Injector.configure(state)

        // Create with matching (Int,String)
        val o1: CtorMatch by new(7, "seven")
        val v1 = o1
        assertEquals(7, v1.seenA)
        assertEquals("seven", v1.seenB)

        // Provision singleton
        val p1: TestListener by provision()
        val p2: TestListener by inject()
        val pp1 = p1
        val pp2 = p2
        println("[DEBUG_LOG] Provisioned singleton TestListener; verifying same instance via inject()")
        assertTrue(pp1 === pp2, "[DEBUG_LOG] inject() should retrieve the same provided singleton instance")
        println("[DEBUG_LOG] testConstructorSelectionAndProvision END")
    }
}
