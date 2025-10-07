package dev.jamiecrown.gdx.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import net.mgsx.gltf.loaders.gltf.GLTFAssetLoader
import net.mgsx.gltf.scene3d.scene.SceneAsset

abstract class TestPrefab(open val config: LibGdxConfig?) {

    /**
     * Called in create() after OpenGL context is created; create resources here.
     */
    abstract fun initialize()


    /**
     * Dipose of necessary resources here; called in dispose method of ApplicationListener
     */
    abstract fun teardown()
}

open class BaseTestPrefab(config: LibGdxConfig?) : TestPrefab(config) {

    lateinit var font: BitmapFont
    lateinit var spriteBatch: SpriteBatch
    lateinit var modelBatch: ModelBatch
    lateinit var environment: Environment
    lateinit var orthographicCamera: OrthographicCamera
    lateinit var camera: PerspectiveCamera
    lateinit var viewport: Viewport
    lateinit var drawer: ShapeRenderer
    lateinit var model: Model
    lateinit var modelInstance: ModelInstance
    lateinit var manager: AssetManager
    lateinit var input : InputAdapter

    override fun initialize() {
        manager = AssetManager()
        manager.setLoader(SceneAsset::class.java, ".gltf", GLTFAssetLoader())
        font = BitmapFont()
        spriteBatch = SpriteBatch()
        modelBatch = ModelBatch()
        environment = Environment()
        orthographicCamera = OrthographicCamera()
        camera = PerspectiveCamera(67f, config!!.width.toFloat(), config!!.height.toFloat())
        camera.position.set(10f, 10f, 10f)
        camera.lookAt(0f, 0f, 0f)
        camera.near = 1f
        camera.far = 300f
        camera.update()
        viewport = ScreenViewport(camera)
        viewport.update(config!!.width, config!!.height, true)
        drawer = ShapeRenderer()
        model = createModel() // create a default cube model
        modelInstance = ModelInstance(model)
        input = FirstPersonCameraController(camera)
        Gdx.input.inputProcessor = input

    }

    fun createModel(path: String? = null) : Model {
        // Create a simple cube model if no path is provided
        if (path == null || path.isEmpty()) {
            val modelBuilder = com.badlogic.gdx.graphics.g3d.utils.ModelBuilder()
            return modelBuilder.createBox(1f,1f,1f, Material(),
                (com.badlogic.gdx.graphics.VertexAttributes.Usage.Position or com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal).toLong())

        } else {
            manager.load(path, Model::class.java)
            manager.finishLoading()
            return manager.get(path, Model::class.java)
        }
    }

    fun clear() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
    }

    override fun teardown() {
        // Dispose of resources here
    }
}