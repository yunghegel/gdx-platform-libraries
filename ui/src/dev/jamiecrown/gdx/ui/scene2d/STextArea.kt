package dev.jamiecrown.gdx.ui.scene2d
import com.badlogic.gdx.scenes.scene2d.ui.TextArea
import dev.jamiecrown.gdx.ui.UI

open class STextArea(text: String? = null, style : String = "textArea") : TextArea(text,UI.skin,style) {

    fun sanitizeCarriageReturn(current: String?) {
        text = current?.replace("\r", "")
    }

//    override fun setText(str: String?) {
//        sanitizeCarriageReturn(str!!)
//        super.setText(str)
//    }

}