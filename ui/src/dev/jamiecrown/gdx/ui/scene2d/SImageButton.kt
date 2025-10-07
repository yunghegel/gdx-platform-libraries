package dev.jamiecrown.gdx.ui.scene2d
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import dev.jamiecrown.gdx.ui.UI
open class SImageButton(style: ImageButtonStyle) : ImageButton(style) {

    constructor(style: String) : this(UI.skin.get(style,ImageButtonStyle::class.java))

    constructor(up: String,bg:String?=null) : this(create(up,bg))

    constructor(drawable: Drawable) : this(UI.skin.get("default",ImageButtonStyle::class.java).apply { imageUp = drawable })

    companion object {
        fun create(up: String,bg:String?=null): ImageButtonStyle {
            val style = ImageButtonStyle()
            style.imageUp = UI.skin.getDrawable(up)
            bg?.let {
                style.up = UI.skin.getDrawable(bg)
            }
            return style
        }

        fun create(drawable: Drawable): ImageButtonStyle {
            return ImageButtonStyle().apply { imageUp = drawable }
        }

        fun fromNamedDrawable(name: String) : SImageButton {
            val drawable = UI.skin.getDrawable(name)
            return SImageButton(drawable)
        }
    }

}
