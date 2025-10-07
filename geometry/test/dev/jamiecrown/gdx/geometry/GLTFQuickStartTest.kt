package dev.jamiecrown.gdx.geometry

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cubemap
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import net.mgsx.gltf.loaders.gltf.GLTFLoader
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.scene.SceneSkybox
import net.mgsx.gltf.scene3d.utils.IBLBuilder
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import dev.jamiecrown.gdx.test.LibGdxConfig
import dev.jamiecrown.gdx.test.LibGdxTestContext
import dev.jamiecrown.gdx.test.LibGdxTestExtension

/**
 * Converts the GLTF quick start sample into an executable test using our LibGdx test framework.
 * The test runs a simple scene for a few frames to ensure the GLTF pipeline initializes and renders without errors.
 */
@ExtendWith(LibGdxTestExtension::class)
@LibGdxConfig(mode = LibGdxConfig.Mode.WINDOWED, width = 640, height = 480, frames = -1, title = "GLTF QuickStart Test")
class GLTFQuickStartTest {

    @Test
    fun rendersGltfScene(context: LibGdxTestContext) {
        // Test-local state captured by the DSL lambdas
        var sceneManager: SceneManager? = null
        var sceneAsset: SceneAsset? = null
        var scene: Scene? = null
        var camera: PerspectiveCamera? = null
        var diffuseCubemap: Cubemap? = null
        var environmentCubemap: Cubemap? = null
        var specularCubemap: Cubemap? = null
        var brdfLUT: Texture? = null
        var skybox: SceneSkybox? = null
        var light: DirectionalLightEx? = null
        var time = 0f

        context.create {
            // Load a model. Prefer config.modelPath if set; fallback to a known sample in geometry resources.
            val path = if (config?.modelPath?.isNotEmpty() == true) config!!.modelPath else "models/gltf/Torus.gltf"
            sceneAsset = GLTFLoader().load(Gdx.files.internal(path))
            scene = Scene(sceneAsset!!.scene)
            sceneManager = SceneManager().also { it.addScene(scene) }

            // Camera
            camera = PerspectiveCamera(60f, config!!.width.toFloat(), config!!.height.toFloat())
            camera!!.near = 1f
            camera!!.far = 100f
            sceneManager!!.setCamera(camera)
            camera!!.position.set(3f, 3f, 3f)

            // Light
            light = DirectionalLightEx().apply {
                direction.set(1f, -3f, 1f).nor()
                color.set(Color.WHITE)
            }
            sceneManager!!.environment.add(light)

            // IBL
            val iblBuilder = IBLBuilder.createOutdoor(light)
            environmentCubemap = iblBuilder.buildEnvMap(1024)
            diffuseCubemap = iblBuilder.buildIrradianceMap(256)
            specularCubemap = iblBuilder.buildRadianceMap(10)
            iblBuilder.dispose()

            brdfLUT = Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"))
            sceneManager!!.setAmbientLight(1f)
            sceneManager!!.environment.set(PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT))
            sceneManager!!.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap))
            sceneManager!!.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap))

            // Skybox
            skybox = SceneSkybox(environmentCubemap)
            sceneManager!!.setSkyBox(skybox)

            // Sanity checks
            assertNotNull(sceneAsset)
            assertNotNull(sceneManager)
            assertNotNull(camera)
        }

        context.render {
            val deltaTime = Gdx.graphics.deltaTime
            time += deltaTime

            // animate camera
            camera!!.position.setFromSpherical(MathUtils.PI / 4, time * .3f).scl(-5f)
            camera!!.up.set(Vector3.Y)
            camera!!.lookAt(Vector3.Zero)
            camera!!.update()

            // render
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
            sceneManager!!.update(deltaTime)
            sceneManager!!.render()
        }

        context.dispose {
            // Dispose created resources
            sceneManager?.dispose()
            sceneAsset?.dispose()
            environmentCubemap?.dispose()
            diffuseCubemap?.dispose()
            specularCubemap?.dispose()
            brdfLUT?.dispose()
            skybox?.dispose()
        }

        // Run the app for configured number of frames
        context.run()
    }
}
