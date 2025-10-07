package dev.jamiecrown.gdx.test

import dev.jamiecrown.gdx.test.LibGdxConfig
import dev.jamiecrown.gdx.test.LibGdxTestContext
import dev.jamiecrown.gdx.test.LibGdxTestExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Duration.Companion.seconds

@ExtendWith(LibGdxTestExtension::class)
@LibGdxConfig(mode = LibGdxConfig.Mode.WINDOWED, width = 400, height = 300, frames = 5, gl = LibGdxConfig.GL.GL20)
class MyLibGdxTests {

    @Test
    fun testPerInstanceApp(context: LibGdxTestContext) {
        context.create {
            println("create() called for this test")
        }

        context.render {
            println("render: frame=$frame")
            frame++
            // optional: stop earlier from inside render
            if (frame >= 5) stop()
        }

        // start and block until frames are done (or stop() called)
        context.run()
        println("App finished, continuing test assertions")
    }

    @Test
    @LibGdxConfig(frames = 3) // this test will run until you call stop()
    fun testInfiniteUntilStop(context: LibGdxTestContext) {
        context.render {

            println("render: frame=$frame")
            if (frame >= 3) {
                println("Calling stop() from inside render after 3 frames")
                stop()
            }
        }
        // runs until stop() called inside render
        context.run()
    }
}
