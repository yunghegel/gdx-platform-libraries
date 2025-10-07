package dev.jamiecrown.gdx.cli

import dev.jamiecrown.gdx.state.Injector
import dev.jamiecrown.gdx.state.app.AppStateManager
import dev.jamiecrown.gdx.state.storage.FileKeyValueStore

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@Namespace("math")
class MathCommands {
    @Command(name = "add", description = "Add two integers")
    fun add(@Parameter(name = "a", description = "first") a: Int,
            @Parameter(name = "b", description = "second") b: Int,
            @Option(name = "verbose", description = "print details") verbose: Boolean = false): Int {
        if (verbose) println("[DEBUG_LOG] math add invoked with a=$a b=$b (verbose)")
        return a + b
    }

    @Command(name = "sum", description = "Sum a list of ints")
    fun sum(@Parameter(name = "values", description = "list", required = true) values: List<Int>): Int {
        println("[DEBUG_LOG] math sum invoked with values=$values")
        return values.sum()
    }
}

class MiscCommands {
    @Command(name = "echo", description = "Echo a message")
    fun echo(@Parameter(name = "msg", description = "message") msg: String,
             @Option(name = "times", description = "repeat count", required = false, default = "1") times: Int,
             @Option(name = "upper", description = "uppercase", required = false) upper: Boolean = false): String {
        println("[DEBUG_LOG] echo invoked with msg='$msg', times=$times, upper=$upper")
        val base = if (upper) msg.uppercase() else msg
        return (1..times).joinToString("") { base }
    }
}

class CommandRegistryTest {

    private fun tempDir(): File {
        val dir = File("testdata/CLI-${System.currentTimeMillis()}")
        println("[DEBUG_LOG] Creating non-temp dir for CLI tests: ${dir.absolutePath}")
        dir.mkdirs()
        return dir
    }

    @Test
    fun manualRegistrationAndExecution() {
        println("[DEBUG_LOG] manualRegistrationAndExecution START")
        CommandRegistry.clear()
        val math = MathCommands()
        val misc = MiscCommands()
        CommandRegistry.register(math)
        CommandRegistry.register(misc)

        val r1 = CommandRegistry.execute("math add 2 5 --verbose") as Int
        println("[DEBUG_LOG] math add result=$r1")
        assertEquals(7, r1, "[DEBUG_LOG] Expected 2+5 to equal 7")

        val r2 = CommandRegistry.execute("echo 'hi' --times=3") as String
        println("[DEBUG_LOG] echo result='$r2'")
        assertEquals("hihihi", r2, "[DEBUG_LOG] Expected echo to repeat 3 times")

        val r3 = CommandRegistry.execute("math sum [1,2,3,4]") as Int
        println("[DEBUG_LOG] sum result=$r3")
        assertEquals(10, r3)

        println("[DEBUG_LOG] manualRegistrationAndExecution END")
    }

    @Test
    fun autoDetectionViaInjector() {
        println("[DEBUG_LOG] autoDetectionViaInjector START")
        CommandRegistry.clear()
        val state = AppStateManager(FileKeyValueStore(tempDir()))
        Injector.configure(state)
        val obj: MathCommands by dev.jamiecrown.gdx.state.new()
        val o = obj // trigger creation and thus injector hook
        val list = CommandRegistry.list().map { it.key }
        println("[DEBUG_LOG] After injector, commands registered: $list")
        assertTrue(list.any { it.endsWith(" add") }, "[DEBUG_LOG] Expected 'add' to be registered via Injector")

        val r = CommandRegistry.execute("math add 10 20") as Int
        println("[DEBUG_LOG] result from injector-registered command: $r")
        assertEquals(30, r)
        println("[DEBUG_LOG] autoDetectionViaInjector END")
    }

    @Test
    fun optionAndDefaultsAndErrors() {
        println("[DEBUG_LOG] optionAndDefaultsAndErrors START")
        CommandRegistry.clear()
        CommandRegistry.register(MiscCommands())
        val r = CommandRegistry.execute("echo hey --upper") as String
        println("[DEBUG_LOG] echo upper result='$r'")
        assertEquals("HEY", r)

        try {
            println("[DEBUG_LOG] Expecting error for missing required positional")
            CommandRegistry.execute("echo")
            fail("[DEBUG_LOG] Expected error for missing required param")
        } catch (e: IllegalArgumentException) {
            println("[DEBUG_LOG] Caught expected error: ${e.message}")
        }
        println("[DEBUG_LOG] optionAndDefaultsAndErrors END")
    }
}
