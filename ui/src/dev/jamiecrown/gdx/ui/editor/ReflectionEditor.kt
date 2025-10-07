package dev.jamiecrown.gdx.ui.editor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTextField
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import dev.jamiecrown.gdx.state.Persist
import dev.jamiecrown.gdx.ui.UI
import dev.jamiecrown.gdx.ui.scene2d.ColorBox
import dev.jamiecrown.gdx.ui.scene2d.SCheckBox
import dev.jamiecrown.gdx.ui.scene2d.SLabel
import dev.jamiecrown.gdx.ui.scene2d.STable
import java.lang.reflect.Field

/**
 * A simple reflection-based editor that inspects fields/properties annotated with @Persist
 * (from dev.jamiecrown.gdx.injection) and generates editing controls for them.
 *
 * Currently supported mappings:
 * - String -> VisTextField
 * - Int/Float/Double -> VisTextField (numeric)
 * - Boolean -> SCheckBox
 * - Enum -> VisSelectBox
 * - Color (com.badlogic.gdx.graphics.Color) -> ColorBox
 *
 * Two-way sync helpers: [refreshFromModel] updates widgets from the object, and [applyToModel]
 * writes current widget values back to the object.
 */
class ReflectionEditor(target: Any) : STable() {

    private var target: Any = target
    private val bindings = mutableListOf<Binding>()

    init {
        defaults().pad(4f).left().top()
        rebuild()
    }

    fun setTarget(newTarget: Any) {
        this.target = newTarget
        rebuild()
    }

    /** Rebuild the editor UI based on current [target]. */
    fun rebuild() {
        clearChildren()
        bindings.clear()
        val fields = collectKeptFields(target)
        for (f in fields) {
            val label = SLabel(prettyName(f.name))
            val editor = editorForField(f)
            val row = STable()
            row.add(label).left().width(120f)
            row.add(editor).expandX().fillX().left()
            add(row).expandX().fillX().row()
        }
    }

    /** Updates widget values to reflect current values on the target model. */
    fun refreshFromModel() {
        for (b in bindings) b.updateWidget()
    }

    /** Applies widget values back to the target model. */
    fun applyToModel() {
        for (b in bindings) b.updateModel()
    }

    // region Internals
    private fun collectKeptFields(instance: Any): List<Field> {
        val result = mutableListOf<Field>()
        var c: Class<*>? = instance.javaClass
        while (c != null && c != Any::class.java) {
            for (f in c.declaredFields) {
                if (f.getAnnotation(Persist::class.java) != null) {
                    f.isAccessible = true
                    result += f
                }
            }
            c = c.superclass
        }
        // sort stable by declaration name
        return result.sortedBy { it.name }
    }

    private fun editorForField(field: Field): Actor {
        val type = field.type
        return when {
            type == java.lang.String::class.java -> textFieldBinding(field, numeric = false)
            type == java.lang.Integer.TYPE || type == Integer::class.java -> textFieldBinding(field, numeric = true)
            type == java.lang.Float.TYPE || type == java.lang.Float::class.java -> textFieldBinding(field, numeric = true)
            type == java.lang.Double.TYPE || type == java.lang.Double::class.java -> textFieldBinding(field, numeric = true)
            type == java.lang.Boolean.TYPE || type == java.lang.Boolean::class.java -> booleanBinding(field)
            Color::class.java.isAssignableFrom(type) -> colorBinding(field)
            type.isEnum -> enumBinding(field)
            else -> textFieldBinding(field, numeric = false) // fallback to toString
        }
    }

    private fun textFieldBinding(field: Field, numeric: Boolean): Actor {
        val tf = VisTextField("")
        val b = object : Binding(field, tf) {
            override fun updateWidget() {
                val v = field.get(target)
                tf.text = v?.toString() ?: ""
            }

            override fun updateModel() {
                val text = tf.text ?: ""
                when {
                    field.type == java.lang.String::class.java -> field.set(target, text)
                    field.type == java.lang.Integer.TYPE || field.type == Integer::class.java -> field.set(target, text.toIntOrNull() ?: 0)
                    field.type == java.lang.Float.TYPE || field.type == java.lang.Float::class.java -> field.set(target, text.toFloatOrNull() ?: 0f)
                    field.type == java.lang.Double.TYPE || field.type == java.lang.Double::class.java -> field.set(target, text.toDoubleOrNull() ?: 0.0)
                    else -> field.set(target, text)
                }
            }
        }
        tf.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { b.updateModel() }
        })
        bindings += b
        b.updateWidget()
        return tf
    }

    private fun booleanBinding(field: Field): Actor {
        val cb = SCheckBox("")
        val b = object : Binding(field, cb) {
            override fun updateWidget() {
                cb.isChecked = (field.getBooleanSafe(target) ?: false)
            }
            override fun updateModel() {
                field.setBooleanSafe(target, cb.isChecked)
            }
        }
        cb.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                b.updateModel()
            }
        })
        bindings += b
        b.updateWidget()
        return cb
    }

    private fun enumBinding(field: Field): Actor {
        @Suppress("UNCHECKED_CAST")
        val enumType = field.type as Class<out Enum<*>>
        val select = VisSelectBox<Enum<*>>()
        val values = enumType.enumConstants.toList()
        val arr = com.badlogic.gdx.utils.Array<Enum<*>>()
        arr.addAll(*values.toTypedArray())
        select.items = arr
        val b = object : Binding(field, select) {
            override fun updateWidget() {
                val v = field.get(target) as? Enum<*>
                if (v != null) select.selected = v else if (values.isNotEmpty()) select.selected = values.first()
            }
            override fun updateModel() {
                val sel = select.selected
                if (sel != null) field.set(target, sel)
            }
        }
        select.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { b.updateModel() }
        })
        bindings += b
        b.updateWidget()
        return select
    }

    private fun colorBinding(field: Field): Actor {
        val provider = {
            val c = field.get(target) as? Color
            if (c == null) {
                val created = Color(1f, 1f, 1f, 1f)
                field.set(target, created)
                created
            } else c
        }
        val box = ColorBox(field.name, provider, true, UI.skin)
        val b = object : Binding(field, box) {
            override fun updateWidget() {
                // ColorBox reads from provider; firing change event to refresh visuals
                box.fire(ChangeListener.ChangeEvent())
            }
            override fun updateModel() {
                // ColorBox callback already writes into the color via provider().set(c)
            }
        }
        box.callback = { _ -> b.updateModel() }
        bindings += b
        b.updateWidget()
        return box
    }

    private fun prettyName(name: String): String {
        // Convert camelCase to words: simple heuristic
        return name.replace(Regex("(?<=[a-z0-9])([A-Z])"), " $1").replaceFirstChar { it.uppercase() }
    }

    private abstract inner class Binding(val field: Field, val actor: Actor) {
        abstract fun updateWidget()
        abstract fun updateModel()
    }

    private fun Field.getBooleanSafe(instance: Any): Boolean? = try {
        when (type) {
            java.lang.Boolean.TYPE, java.lang.Boolean::class.java -> get(instance) as? Boolean
            else -> null
        }
    } catch (_: Throwable) { null }

    private fun Field.setBooleanSafe(instance: Any, value: Boolean) {
        try { set(instance, value) } catch (_: Throwable) {}
    }
    // endregion
}
