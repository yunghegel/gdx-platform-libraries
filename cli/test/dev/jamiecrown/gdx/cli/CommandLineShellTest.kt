package dev.jamiecrown.gdx.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CommandLineShellTest {

    @Test
    fun historyAndExecution() {
        println("[DEBUG_LOG] historyAndExecution START")
        CommandRegistry.clear()
        // Reuse test commands from CommandRegistryTest
        val math = MathCommands()
        val misc = MiscCommands()
        CommandRegistry.register(math)
        CommandRegistry.register(misc)
        val shell = CommandLineShell()

        val r1 = shell.submit("math add 3 4")
        println("[DEBUG_LOG] submit result success=${'$'}{r1.success} output=${'$'}{r1.output} error=${'$'}{r1.error}")
        assertTrue(r1.success, "[DEBUG_LOG] Expected execution success for 'math add 3 4'")
        assertEquals(7, r1.output as Int)
        val hist = shell.history()
        println("[DEBUG_LOG] history now has ${'$'}{hist.size} entries: ${'$'}hist")
        assertEquals(listOf("math add 3 4"), hist)
        println("[DEBUG_LOG] historyAndExecution END")
    }

    @Test
    fun completionFromHistoryAndKnown() {
        println("[DEBUG_LOG] completionFromHistoryAndKnown START")
        CommandRegistry.clear()
        CommandRegistry.register(MathCommands())
        CommandRegistry.register(MiscCommands())
        val shell = CommandLineShell()

        // Seed history
        shell.submit("echo hi --times=2")
        val c1 = shell.complete("ec")
        println("[DEBUG_LOG] completion for 'ec' => ${'$'}c1")
        assertEquals("echo", c1, "[DEBUG_LOG] Expected completion to 'echo' from history")

        // From known commands (namespaced)
        val c2 = shell.complete("ma")
        println("[DEBUG_LOG] completion for 'ma' => ${'$'}c2")
        assertEquals("math add", c2, "[DEBUG_LOG] Expected to complete to a full command signature 'math add'")
        println("[DEBUG_LOG] completionFromHistoryAndKnown END")
    }

    @Test
    fun suggestionsOnError() {
        println("[DEBUG_LOG] suggestionsOnError START")
        CommandRegistry.clear()
        CommandRegistry.register(MathCommands())
        val shell = CommandLineShell()

        val bad = shell.submit("math adb 1 2")
        println("[DEBUG_LOG] bad command success=${'$'}{bad.success} error=${'$'}{bad.error} suggestions=${'$'}{bad.suggestions}")
        assertTrue(!bad.success, "[DEBUG_LOG] Expected failure for mistyped command")
        val sug = bad.suggestions
        assertNotNull(sug)
        assertTrue(sug.any { it.contains("math add") }, "[DEBUG_LOG] Expected suggestions to contain 'math add'")
        println("[DEBUG_LOG] suggestionsOnError END")
    }
}
