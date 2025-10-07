package dev.jamiecrown.gdx.ui.scene2d
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import dev.jamiecrown.gdx.ui.UI
class SImage(drawable: Drawable, size: Int = 16) : Image(drawable) {



    constructor(name: String, size: Int = 16) : this(UI.skin.getDrawable(name),size)

    init {
        width = size.toFloat()
        height = size.toFloat()
    }

}
