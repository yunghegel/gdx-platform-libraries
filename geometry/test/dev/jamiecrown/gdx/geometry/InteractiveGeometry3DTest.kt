package dev.jamiecrown.gdx.geometry

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import dev.jamiecrown.gdx.test.LibGdxConfig
import dev.jamiecrown.gdx.test.LibGdxTestContext
import dev.jamiecrown.gdx.test.LibGdxTestExtension
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test

/**
 * Interactive 3D test that renders a grid and a rotating model with basic camera controls.
 * - Mouse + WASD: move/look around (FirstPersonCameraController from BaseTestPrefab)
 * - SPACE: toggle rotation on/off
 * - C: toggle model color between white and orange
 * - ESC or close window: exit test
 */
@ExtendWith(LibGdxTestExtension::class)
class InteractiveGeometry3DTest {

    @LibGdxConfig(
        mode = LibGdxConfig.Mode.WINDOWED,
        width = 1024,
        height = 720,
        title = "Interactive Geometry 3D",
        gl = LibGdxConfig.GL.GL30,
        frames = -1
    )
    @Test
    fun interactive3D(context: LibGdxTestContext) {
        // Local state for this test
        var environment: Environment = Environment()
        var rotating = true
        var angle = 0f
        var gridSize = 20
        var gridSpacing = 1f
        var baseColor = Color.WHITE.cpy()

        // Replace default cube with a simple sphere to show normals/lighting clearly
        lateinit var model: Model
        lateinit var modelInstance: ModelInstance

        context.onCreate = {
            // Camera comes from BaseTestPrefab; positioned at (10,10,10) looking at origin
            // Set up environment/lighting
            environment.add(DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f))

            // Build a lit sphere
            val mb = ModelBuilder()
            model = mb.createSphere(
                2f, 2f, 2f, 32, 24,
                com.badlogic.gdx.graphics.g3d.Material(ColorAttribute.createDiffuse(baseColor)),
                (com.badlogic.gdx.graphics.VertexAttributes.Usage.Position or com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal).toLong()
            )
            modelInstance = ModelInstance(model)
            // Raise it slightly above the grid
            modelInstance.transform.setToTranslation(0f, 1f, 0f)
        }

        context.onRender = {
            // input handling
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                stop()
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                rotating = !rotating
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
                baseColor = if (baseColor == Color.WHITE) Color.ORANGE else Color.WHITE
                val mat = modelInstance.materials.first()
                mat.set(ColorAttribute.createDiffuse(baseColor))
            }

            // Update camera
            camera.update()

            // Draw 3D grid
            drawer.projectionMatrix = camera.combined
            drawer.begin(ShapeRenderer.ShapeType.Line)
            drawer.color = Color.DARK_GRAY
            // Draw XZ grid centered at origin
            val half = gridSize / 2
            for (i in -half..half) {
                val x = i * gridSpacing
                val z = i * gridSpacing
                // lines parallel to X (varying Z)
                drawer.line(-half * gridSpacing, 0f, z, half * gridSpacing, 0f, z)
                // lines parallel to Z (varying X)
                drawer.line(x, 0f, -half * gridSpacing, x, 0f, half * gridSpacing)
            }
            // Axes
            drawer.color = Color.RED
            drawer.line(0f, 0f, 0f, 3f, 0f, 0f)
            drawer.color = Color.GREEN
            drawer.line(0f, 0f, 0f, 0f, 3f, 0f)
            drawer.color = Color.BLUE
            drawer.line(0f, 0f, 0f, 0f, 0f, 3f)
            drawer.end()

            // Rotate model if enabled
            if (rotating) {
                angle = (angle + Gdx.graphics.deltaTime * 30f) % 360f
                val pos = Vector3()
                modelInstance.transform.getTranslation(pos)
                modelInstance.transform.idt().translate(pos).rotate(Vector3.Y, angle)
            }

            // Render model with lighting
            modelBatch.begin(camera)
            modelBatch.render(modelInstance, environment)
            modelBatch.end()

            // UI overlay
            spriteBatch.begin()
            font.color = Color.WHITE
            font.draw(
                spriteBatch,
                "Interactive 3D: WASD+Mouse look | SPACE rotate=$rotating | C color | ESC quit",
                10f,
                20f
            )
            spriteBatch.end()
        }

        context.onDispose = {
            // Cleanup
            model.dispose()
        }

        context.run()
    }
}
