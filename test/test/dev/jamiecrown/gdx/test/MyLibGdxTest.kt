package dev.jamiecrown.gdx.test

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@LibGdxConfig(
    mode = LibGdxConfig.Mode.WINDOWED,
    width = 800,
    height = 600,
    title = "Windowed Test",
    gl = LibGdxConfig.GL.GL30,
    frames = 60
)
@ExtendWith(LibGdxTestExtension::class)
class MyLibGdxTest {

    @Test
    fun testWithDSL(context: LibGdxTestContext) {
        context.onCreate = {
            println("Running in create()")
        }

        context.onRender = {
            System.exit(0)
        }

        context.onDispose = {
            println("Cleanup on dispose()")
        }
    }

    @Test
    @LibGdxConfig(LibGdxConfig.Mode.WINDOWED, width = 400, height = 300, frames = -1, gl = LibGdxConfig.GL.GL20)
    fun `test prefab creation and usage`(context: LibGdxTestContext) {
        context.onCreate = {

        }
        context.onRender = {
            modelBatch.render(modelInstance)
            spriteBatch.begin()
            font.draw(spriteBatch, "Testing prefab modelInstance", 10f, 20f)
            spriteBatch.end()
        }
        context.run()
    }
}

