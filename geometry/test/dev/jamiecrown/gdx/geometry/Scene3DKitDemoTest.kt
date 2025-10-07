package dev.jamiecrown.gdx.geometry

import dev.jamiecrown.gdx.test.LibGdxConfig
import dev.jamiecrown.gdx.test.LibGdxTestContext
import dev.jamiecrown.gdx.test.LibGdxTestExtension
import dev.jamiecrown.gdx.test.Scene3DOptions
import dev.jamiecrown.gdx.test.installScene3D
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(LibGdxTestExtension::class)
@LibGdxConfig(mode = LibGdxConfig.Mode.WINDOWED, width = 640, height = 480, frames = 60, title = "Scene3D Kit Demo")
class Scene3DKitDemoTest {

    @Test
    fun runsSceneWithDefaultAnimation(context: LibGdxTestContext) {
        // Quick one-liner to set up a debug-friendly 3D scene
        context.installScene3D(Scene3DOptions(modelPath = "models/gltf/Torus.gltf", animateCamera = true))

        // Run the app for configured number of frames
        context.run()
    }
}
