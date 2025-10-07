package dev.jamiecrown.gdx.ui.scene2d
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import dev.jamiecrown.gdx.ui.UI
open class STextField(text: String, style: String) : TextField(text, UI.skin,style) {

    constructor(text: String, onChange: (String) -> Unit) : this(text) {
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                onChange(getText())
            }
        })
    }

    constructor(text: String) : this(text, "default")

    fun cancelFocus() {
        stage?.setKeyboardFocus(null)
    }

}
