package dev.jamiecrown.gdx.geometry

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import ktx.math.compareTo
import net.mgsx.gltf.loaders.gltf.GLTFLoader
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.scene.SceneSkybox
import net.mgsx.gltf.scene3d.utils.IBLBuilder

class GLTFQuickStartExample : ApplicationAdapter() {
    private var sceneManager: SceneManager? = null
    private var sceneAsset: SceneAsset? = null
    private var scene: Scene? = null
    private var camera: PerspectiveCamera? = null
    private var diffuseCubemap: Cubemap? = null
    private var environmentCubemap: Cubemap? = null
    private var specularCubemap: Cubemap? = null
    private var brdfLUT: Texture? = null
    private var time = 0f
    private var skybox: SceneSkybox? = null
    private var light: DirectionalLightEx? = null

    override fun create() {
        // create scene

        sceneAsset = GLTFLoader().load(Gdx.files.internal("models/gltf/logo.gltf"))
        scene = Scene(sceneAsset!!.scene)
        sceneManager = SceneManager()
        sceneManager!!.addScene(scene)


        // setup camera (The BoomBox model is very small so you may need to adapt camera settings for your scene)
        camera = PerspectiveCamera(60f, Gdx.graphics.getWidth().toFloat(), Gdx.graphics.getHeight().toFloat())
        val d = 3f
        camera!!.near = 1f
        camera!!.far = 100f
        sceneManager!!.setCamera(camera)

        camera?.position?.set(d, d, d)


        // setup light
        light = DirectionalLightEx()
        light!!.direction.set(1f, -3f, 1f).nor()
        light!!.color.set(Color.WHITE)
        sceneManager!!.environment.add(light)


        // setup quick IBL (image based lighting)
        val iblBuilder = IBLBuilder.createOutdoor(light)
        environmentCubemap = iblBuilder.buildEnvMap(1024)
        diffuseCubemap = iblBuilder.buildIrradianceMap(256)
        specularCubemap = iblBuilder.buildRadianceMap(10)
        iblBuilder.dispose()


        // This texture is provided by the library, no need to have it in your assets.
        brdfLUT = Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"))

        sceneManager!!.setAmbientLight(1f)
        sceneManager!!.environment.set(PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT))
        sceneManager!!.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap))
        sceneManager!!.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap))


        // setup skybox
        skybox = SceneSkybox(environmentCubemap)
        sceneManager!!.setSkyBox(skybox)
    }

    override fun resize(width: Int, height: Int) {
        sceneManager!!.updateViewport(width.toFloat(), height.toFloat())
    }

    override fun render() {
        val deltaTime = Gdx.graphics.getDeltaTime()
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

    override fun dispose() {
        sceneManager!!.dispose()
        sceneAsset!!.dispose()
        environmentCubemap!!.dispose()
        diffuseCubemap!!.dispose()
        specularCubemap!!.dispose()
        brdfLUT!!.dispose()
        skybox!!.dispose()
    }
}

