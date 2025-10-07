package dev.jamiecrown.gdx.ui.editor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTextField
import dev.jamiecrown.gdx.test.LibGdxConfig
import dev.jamiecrown.gdx.test.LibGdxTestContext
import dev.jamiecrown.gdx.test.LibGdxTestExtension
import dev.jamiecrown.gdx.ui.scene2d.SCheckBox
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(LibGdxTestExtension::class)
class ReflectionEditorTest {

    enum class Mode { A, B, C }

    class Model {
        @dev.jamiecrown.gdx.state.Persist var name: String = "initial"
        @dev.jamiecrown.gdx.state.Persist var count: Int = 1
        @dev.jamiecrown.gdx.state.Persist var enabled: Boolean = false
        @dev.jamiecrown.gdx.state.Persist var mode: Mode = Mode.A
        @dev.jamiecrown.gdx.state.Persist var tint: Color = Color(0.1f, 0.2f, 0.3f, 0.4f)
    }

    @Test
    @LibGdxConfig(mode = LibGdxConfig.Mode.HEADLESS, frames = 1)
    fun buildEditorAndApply(ctx: LibGdxTestContext) {
        println("[DEBUG_LOG] Starting ReflectionEditorTest.buildEditorAndApply")
        val model = Model()
        var editor: ReflectionEditor? = null
        ctx.create {
            println("[DEBUG_LOG] Creating ReflectionEditor for model with initial fields: name='${model.name}', count=${model.count}, enabled=${model.enabled}, mode=${model.mode}, tint=${model.tint}")
            editor = ReflectionEditor(model)
            println("[DEBUG_LOG] Editor built. Children count: ${editor!!.children.size}")

            val e = editor!!
            // Grab first VisTextField (name)
            val textField = findChildOfType(e, VisTextField::class.java)
            println("[DEBUG_LOG] Found VisTextField: ${textField != null}")
            textField!!.text = "updated"
            // Grab next VisTextField (for count)
            val secondText = findChildOfTypeAfter(e, textField, VisTextField::class.java)
            println("[DEBUG_LOG] Found numeric VisTextField for count: ${secondText != null}")
            secondText!!.text = "42"
            // Grab checkbox
            val check = findChildOfTypeAfter(e, secondText, SCheckBox::class.java)
            println("[DEBUG_LOG] Found SCheckBox: ${check != null}")
            check!!.isChecked = true
            // Grab select box
            val select = findChildOfTypeAfter(e, check, VisSelectBox::class.java)
            println("[DEBUG_LOG] Found VisSelectBox: ${select != null}")
            @Suppress("UNCHECKED_CAST")
            (select as VisSelectBox<Enum<*>>).setSelected(Mode.C)

            // Apply UI -> model
            e.applyToModel()

            assertEquals("updated", model.name)
            assertEquals(42, model.count)
            assertTrue(model.enabled)
            assertEquals(Mode.C, model.mode)
            println("[DEBUG_LOG] Completed ReflectionEditorTest.buildEditorAndApply")
        }
        ctx.start()

        // The editor contains rows (tables). Dive to find widgets.

    }

    @Test
    @LibGdxConfig(mode = LibGdxConfig.Mode.HEADLESS, frames = 1)
    fun refreshFromModelUpdatesWidgets(ctx: LibGdxTestContext) {
        println("[DEBUG_LOG] Starting ReflectionEditorTest.refreshFromModelUpdatesWidgets")
        val model = Model()
        var editor: ReflectionEditor? = null
        ctx.create {
            editor = ReflectionEditor(model)

            val e = editor!!

            // Change the model
            model.name = "abc"
            model.count = 7
            model.enabled = true
            model.mode = Mode.B
            model.tint.set(0.9f, 0.8f, 0.7f, 0.6f)

            // Refresh UI
            e.refreshFromModel()

            val tfName: VisTextField = findChildOfType(e, VisTextField::class.java)!!
            val tfCount: VisTextField = findChildOfTypeAfter(e, tfName, VisTextField::class.java)!!
            val cb: SCheckBox = findChildOfTypeAfter(e, tfCount, SCheckBox::class.java)!!
            val sb: VisSelectBox<*> = findChildOfTypeAfter(e, cb, VisSelectBox::class.java)!!

            println("[DEBUG_LOG] After refresh: tfName='${tfName.text}', tfCount='${tfCount.text}', cb.checked=${cb.isChecked}, sb.selected=${(sb as VisSelectBox<*>).selected}")
            assertEquals("abc", tfName.text)
            assertEquals("7", tfCount.text)
            assertTrue(cb.isChecked)
            @Suppress("UNCHECKED_CAST")
            assertEquals(Mode.B, (sb as VisSelectBox<Enum<*>>).selected)
            println("[DEBUG_LOG] Completed ReflectionEditorTest.refreshFromModelUpdatesWidgets")

        }
        ctx.start()
    }

    // Helpers to traverse children depth-first using non-inline class-based matching
    private fun <T: Actor> findChildOfType(root: Actor, clazz: Class<T>): T? {
        val stack = java.util.ArrayDeque<Actor>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val a = stack.removeFirst()
            if (clazz.isInstance(a)) return clazz.cast(a)
            if (a is com.badlogic.gdx.scenes.scene2d.Group) {
                a.children.forEach { stack.addLast(it) }
            }
        }
        return null
    }

    private fun <T: Actor> findChildOfTypeAfter(root: Actor, after: Actor, clazz: Class<T>): T? {
        var foundAfter = false
        val stack = java.util.ArrayDeque<Actor>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val a = stack.removeFirst()
            if (a === after) { foundAfter = true }
            else if (foundAfter && clazz.isInstance(a)) return clazz.cast(a)
            if (a is com.badlogic.gdx.scenes.scene2d.Group) {
                a.children.forEach { stack.addLast(it) }
            }
        }
        return null
    }
}
