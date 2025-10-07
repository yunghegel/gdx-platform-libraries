package dev.jamiecrown.gdx.state

import dev.jamiecrown.gdx.state.eventbus.EventBus
import dev.jamiecrown.gdx.state.eventbus.Subscribe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EventBusTest {

    open class BaseEvent(val payload: String)
    class ChildEvent(payload: String) : BaseEvent(payload)

    class Listener {
        val received = mutableListOf<String>()

        @Subscribe
        fun onBase(e: BaseEvent) {
            println("[DEBUG_LOG] Listener.onBase received: ${'$'}{e.payload}")
            received += "base:${e.payload}"
        }

        @Subscribe
        fun onChild(e: ChildEvent) {
            println("[DEBUG_LOG] Listener.onChild received: ${'$'}{e.payload}")
            received += "child:${e.payload}"
        }
    }

    @Test
    fun testPostAndUnregister() {
        println("[DEBUG_LOG] Starting EventBusTest.testPostAndUnregister")
        val l = Listener()
        EventBus.register(l)
        EventBus.post(BaseEvent("A"))
        EventBus.post(ChildEvent("B"))
        assertEquals(listOf("base:A", "child:B", "base:B"), l.received)
        EventBus.unregister(l)
        EventBus.post(BaseEvent("C"))
        assertEquals(3, l.received.size)
        println("[DEBUG_LOG] Completed EventBusTest.testPostAndUnregister")
    }
}
