package dev.jamiecrown.gdx.geometry

import dev.jamiecrown.gdx.test.LibGdxConfig
import dev.jamiecrown.gdx.test.LibGdxTestContext
import dev.jamiecrown.gdx.test.LibGdxTestExtension
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test

@ExtendWith(LibGdxTestExtension::class)
class BMeshTests {
    // TODO add tests for BMesh, Vertex, Edge, Face

    @LibGdxConfig(mode = LibGdxConfig.Mode.WINDOWED, width = 400, height = 300, frames = -1, gl = LibGdxConfig.GL.GL20, modelPath = "gltf/Cylinder.gltf")
    @Test
    fun `test load gltf`(context: LibGdxTestContext) {
        context.render {

            camera.update()
            camera.lookAt(0f,0f,0f)
            modelBatch.render(modelInstance)
        }
        context.run()
    }
}

