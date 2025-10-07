package dev.jamiecrown.gdx.ui.scene2d
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import dev.jamiecrown.gdx.ui.UI


class SDialog(val title: String, skin: Skin = UI.skin) : Dialog(title,skin)  {


    init {
        pad(0f)
        style.background.topHeight.coerceAtMost(style.titleFont.lineHeight)
    }

    fun centerOn(actor: Actor) {
        setPosition(actor.x + actor.width / 2 - width / 2, actor.y + actor.height / 2 - height / 2)
    }

    fun show(actor: Actor) {
        centerOn(actor)
        show(actor.stage)
    }

    fun autoremove() {
        addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                remove()
                return super.touchDown(event, x, y, pointer, button)
            }

            override fun keyTyped(event: InputEvent?, character: Char): Boolean {
                remove()
                return super.keyTyped(event, character)
            }
        })
    }

    override fun getPadTop(): Float {
        return style.titleFont.data.lineHeight
    }
}