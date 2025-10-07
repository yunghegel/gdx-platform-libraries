package dev.jamiecrown.gdx.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.kotcrab.vis.ui.VisUI
import com.ray3k.stripe.FreeTypeSkin
import ktx.scene2d.Scene2DSkin

object UI {

    const val SKIN_PATH = "skin/uiskin.json"

    private var _skin: Skin? = null
    val skin: Skin
        get() = _skin ?: load()

    val dnd: DragAndDrop by lazy { DragAndDrop() }
    val font: BitmapFont by lazy { skin.getFont("default") }
    val stage by lazy { Stage() }

    fun load(): Skin {
        _skin?.let { return it }
        val s = FreeTypeSkin(Gdx.files.internal(SKIN_PATH))
        VisUI.load(s)
        Scene2DSkin.defaultSkin = s
        s.jsonClassTags.forEach { entry ->
            val styles = s.getAll(entry.value)
            println(styles)
        }
        _skin = s
        return s
    }

    fun drawable(name: String, color: Color?, configPatch: (NinePatch.()->Unit)? = null) : Drawable {
        val drawable = skin.getDrawable(name)
        if (drawable is Skin.TintedDrawable && color!=null) {
            (drawable as Skin.TintedDrawable).color = color
        }
        if (drawable is NinePatchDrawable && configPatch!=null) {
            (drawable).patch.configPatch()
        }
        return drawable
    }

    fun drawable(name: String, color: Color) : Drawable {
        return drawable(name,color,null)
    }

    fun drawable(name: String) : Drawable {
        return drawable(name,null,null)
    }

}