package dev.jamiecrown.gdx.test

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

/**
 * Lightweight testing utility to quickly spin up a 3D GLTF scene within LibGdxTestContext.
 *
 * Usage:
 *   @ExtendWith(LibGdxTestExtension::class)
 *   @LibGdxConfig(mode = LibGdxConfig.Mode.WINDOWED, frames = 120)
 *   class MySceneTest {
 *       @Test fun run(context: LibGdxTestContext) {
 *           val handles = context.installScene3D(Scene3DOptions(modelPath = "models/gltf/Torus.gltf"))
 *           // Optionally customize in-place using handles in create/render hooks you add after installation
 *           context.run()
 *       }
 *   }
 */

data class Scene3DOptions(
    val modelPath: String? = null,
    val animateCamera: Boolean = true,
    val cameraFov: Float = 60f,
    val cameraNear: Float = 1f,
    val cameraFar: Float = 100f,
    val ambientLight: Float = 1f
)

class Scene3DHandles {
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
    internal var time: Float = 0f
}

fun LibGdxTestContext.installScene3D(options: Scene3DOptions = Scene3DOptions()): Scene3DHandles {
    val handles = Scene3DHandles()

    // Chain existing callbacks if any
    val prevCreate = this.onCreate
    val prevRender = this.onRender
    val prevDispose = this.onDispose

    this.create {
        // Call previously registered create first
        prevCreate?.invoke(this)

        // Load a model. Prefer explicit options.modelPath, then config.modelPath, then default sample path.
        val path = when {
            !options.modelPath.isNullOrBlank() -> options.modelPath
            this.config?.modelPath?.isNotBlank() == true -> this.config!!.modelPath
            else -> "models/gltf/Torus.gltf"
        }

        handles.sceneAsset = GLTFLoader().load(Gdx.files.internal(path))
        handles.scene = Scene(handles.sceneAsset!!.scene)
        handles.sceneManager = SceneManager().also { it.addScene(handles.scene) }

        // Camera
        handles.camera = PerspectiveCamera(options.cameraFov, this.config!!.width.toFloat(), this.config!!.height.toFloat()).apply {
            near = options.cameraNear
            far = options.cameraFar
            position.set(3f, 3f, 3f)
        }
        handles.sceneManager!!.setCamera(handles.camera)

        // Light
        handles.light = DirectionalLightEx().apply {
            direction.set(1f, -3f, 1f).nor()
            color.set(Color.WHITE)
        }
        handles.sceneManager!!.environment.add(handles.light)

        // IBL
        val iblBuilder = IBLBuilder.createOutdoor(handles.light)
        handles.environmentCubemap = iblBuilder.buildEnvMap(1024)
        handles.diffuseCubemap = iblBuilder.buildIrradianceMap(256)
        handles.specularCubemap = iblBuilder.buildRadianceMap(10)
        iblBuilder.dispose()

        // BRDF LUT provided by library
        handles.brdfLUT = Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"))
        handles.sceneManager!!.setAmbientLight(options.ambientLight)
        handles.sceneManager!!.environment.set(PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, handles.brdfLUT))
        handles.sceneManager!!.environment.set(PBRCubemapAttribute.createSpecularEnv(handles.specularCubemap))
        handles.sceneManager!!.environment.set(PBRCubemapAttribute.createDiffuseEnv(handles.diffuseCubemap))

        // Skybox
        handles.skybox = SceneSkybox(handles.environmentCubemap)
        handles.sceneManager!!.setSkyBox(handles.skybox)
    }

    this.render {
        // Call previously registered render first (note: framework has already cleared buffers before calling onRender)
        prevRender?.invoke(this)

        val dt = Gdx.graphics.deltaTime
        handles.time += dt

        // Default camera animation (optional)
        if (options.animateCamera) {
            handles.camera?.let { cam ->
                cam.position.setFromSpherical(MathUtils.PI / 4, handles.time * .3f).scl(-5f)
                cam.up.set(Vector3.Y)
                cam.lookAt(Vector3.Zero)
                cam.update()
            }
        }

        // Render
        handles.sceneManager?.let { sm ->
            // Not clearing here; framework has already cleared to default color.
            sm.update(dt)
            sm.render()
        }
    }

    this.dispose {
        // Call previously registered dispose first
        prevDispose?.invoke(this)
        try {
            handles.sceneManager?.dispose()
            handles.sceneAsset?.dispose()
            handles.environmentCubemap?.dispose()
            handles.diffuseCubemap?.dispose()
            handles.specularCubemap?.dispose()
            handles.brdfLUT?.dispose()
            handles.skybox?.dispose()
        } catch (ignored: Throwable) {
            // best effort
        }
    }

    return handles
}
