package dev.jamiecrown.gdx.ui.scene2d
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import dev.jamiecrown.gdx.ui.UI
import ktx.actors.onChange

open class SCheckBox(label:String) : CheckBox(label, UI.skin) {

    constructor(label:String,checked:Boolean) : this(label) {
        isChecked = checked
    }

    constructor(label:String,checked:Boolean,listener: (Boolean) -> Unit) : this(label,checked) {
        onChange {
            listener(isChecked)
            true
        }
    }

}