package dev.jamiecrown.gdx.state

import dev.jamiecrown.gdx.state.eventbus.EventBus
import dev.jamiecrown.gdx.state.eventbus.Listen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EventBusNamedEventsTest {

    class NamedListener {
        val received = mutableListOf<String>()

        @Listen("ping")
        fun onPing(data: Any) {
            println("[DEBUG_LOG] NamedListener.onPing received data=$data")
            received += "ping:$data"
        }

        @Listen("noparam")
        fun onNoParam() {
            println("[DEBUG_LOG] NamedListener.onNoParam invoked")
            received += "noparam"
        }

        @Listen("typed")
        fun onTyped(msg: String) {
            println("[DEBUG_LOG] NamedListener.onTyped received msg='$msg'")
            received += "typed:$msg"
        }
    }

    @Test
    fun testNamedEvents() {
        println("[DEBUG_LOG] Starting EventBusNamedEventsTest.testNamedEvents")
        val l = NamedListener()
        EventBus.register(l)

        EventBus.post("ping", 123)
        EventBus.post("noparam")
        EventBus.post("typed", "hello")
        // Should not deliver due to type mismatch
        EventBus.post("typed", 42)

        assertEquals(listOf("ping:123", "noparam", "typed:hello"), l.received)
        EventBus.unregister(l)
        EventBus.post("ping", 999)
        assertEquals(3, l.received.size)
        println("[DEBUG_LOG] Completed EventBusNamedEventsTest.testNamedEvents")
    }
}
