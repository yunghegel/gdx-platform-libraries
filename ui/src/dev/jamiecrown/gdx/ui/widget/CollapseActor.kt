package dev.jamiecrown.gdx.ui.widget

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import dev.jamiecrown.gdx.ui.UI
import dev.jamiecrown.gdx.ui.scene2d.STable
import dev.jamiecrown.gdx.ui.scene2d.STextButton

class CollapseActor(val index : Int, val stack : Stack, val actor: Actor,val pane: MultiPane) : STable() {

    val buttonText : () -> String = {
        if (actor.isVisible) {
            "-"
        } else {
            "+"
        }
    }

    val OFFSET = 10f

    val position : (Stack, Int) -> Pair<Float, Float> = { stack, index ->
        when (index) {
            0 -> {
                Pair(stack.width  + stack.x + OFFSET, stack.y)
            }
            1 -> {
                Pair(stack.x, stack.y + stack.height + OFFSET)
            }
            else -> {
                Pair(stack.x, stack.y - stack.height - OFFSET)
            }
        }
    }

    init {
        addActor(createTextButton())
        actor.setPosition(0f, 0f)
        actor.setSize(stack.width, stack.height)
        actor.isVisible = false
        actor.setZIndex(1)
        stack.add(this)
        setSize(stack.width, stack.height)
        setPosition(position(stack, index).first, position(stack, index).second)

    }

    fun createTextButton() : Actor {
        val button = STextButton(buttonText())
        UI.stage.addActor(actor)

        button.addListener {
actor.isVisible     = !actor.isVisible
            if (actor.isVisible) {
                button.setText("-")
            } else {
                button.setText("+")
            }
            true  }
        return button
     }

}