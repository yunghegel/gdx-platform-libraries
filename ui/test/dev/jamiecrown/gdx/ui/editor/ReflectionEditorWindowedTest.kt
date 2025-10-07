package dev.jamiecrown.gdx.ui.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import dev.jamiecrown.gdx.test.LibGdxConfig
import dev.jamiecrown.gdx.test.LibGdxTestContext
import dev.jamiecrown.gdx.test.LibGdxTestExtension
import dev.jamiecrown.gdx.ui.UI
import dev.jamiecrown.gdx.ui.scene2d.STable
import dev.jamiecrown.gdx.ui.scene2d.SWindow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(LibGdxTestExtension::class)
class ReflectionEditorWindowedTest {

    enum class Mode { A, B, C }

    class Model {
        @dev.jamiecrown.gdx.state.Persist var name: String = "windowed"
        @dev.jamiecrown.gdx.state.Persist var count: Int = 3
        @dev.jamiecrown.gdx.state.Persist var enabled: Boolean = true
        @dev.jamiecrown.gdx.state.Persist var mode: Mode = Mode.B
        @dev.jamiecrown.gdx.state.Persist var tint: com.badlogic.gdx.graphics.Color = com.badlogic.gdx.graphics.Color(0.2f, 0.6f, 0.8f, 1f)
    }

    @Test
    @LibGdxConfig(mode = LibGdxConfig.Mode.WINDOWED, frames = -1, width = 800, height = 600, title = "ReflectionEditor Windowed Test")
    fun showEditorWindowed(ctx: LibGdxTestContext) {
        println("[DEBUG_LOG] Starting ReflectionEditorWindowedTest.showEditorWindowed")
        val model = Model()
        var stage: Stage? = null
        var editor: ReflectionEditor? = null

        ctx.create {
            // Initialize a stage and add the editor
            stage = UI.stage
            stage!!.viewport = ScreenViewport()
            val root = STable().apply { setFillParent(true)}
            stage!!.addActor(root)
            root.add(SWindow("Test").apply {
                setSize(400f, 300f)
                setPosition(50f, 50f)
                isMovable = true
                isResizable = true
                isModal = false
            })
            editor = ReflectionEditor(model)
            editor!!.pack()
            editor!!.setPosition(20f, Gdx.graphics.height - editor!!.height - 20f)
            root!!.addActor(editor)
            Gdx.input.inputProcessor = stage
            println("[DEBUG_LOG] Editor created with size "+editor!!.width+"x"+editor!!.height)
        }

        ctx.render {
            // Animate something minor to show interactivity
            clear()
            stage?.act(Gdx.graphics.deltaTime)
            stage?.draw()

        }

        ctx.dispose {
            println("[DEBUG_LOG] Disposing stage and completing windowed test")
            stage?.dispose()
        }

        ctx.start()
        println("[DEBUG_LOG] Completed ReflectionEditorWindowedTest.showEditorWindowed")
    }
}
