package dev.jamiecrown.gdx.state

import dev.jamiecrown.gdx.state.app.AppStateManager
import dev.jamiecrown.gdx.state.eventbus.EventBus
import dev.jamiecrown.gdx.state.eventbus.Listen
import dev.jamiecrown.gdx.state.storage.FileKeyValueStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class InjectorNamedEventsTest {

    private fun tempDir(): File {
        val dir = File("testdata/InjectorNamedEventsTest-" + System.currentTimeMillis())
        println("[DEBUG_LOG] Creating dir: ${dir.absolutePath}")
        dir.mkdirs()
        return dir
    }

    class NamedByInjector {
        var last: String? = null

        @Listen("evt")
        fun onEvt(msg: String) {
            println("[DEBUG_LOG] NamedByInjector.onEvt('$msg')")
            last = msg
        }
    }

    @Test
    fun testAutoRegistrationForNamedEvents() {
        println("[DEBUG_LOG] Starting InjectorNamedEventsTest.testAutoRegistrationForNamedEvents")
        val state = AppStateManager(FileKeyValueStore(tempDir()))
        Injector.configure(state)

        val inst: NamedByInjector by new()
        val obj = inst // realize
        EventBus.post("evt", "hello")
        assertEquals("hello", obj.last, "[DEBUG_LOG] Expected listener to receive named event via Injector auto-registration")
        println("[DEBUG_LOG] Completed InjectorNamedEventsTest.testAutoRegistrationForNamedEvents")
    }
}
