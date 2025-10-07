package dev.jamiecrown.gdx.ui.scene2d

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.Tooltip

class STooltip(stylename: String) : Tooltip(stylename){

    val props = TooltipProps()

    class TooltipProps {
        var targetActorX: Float = 0f
        var targetActorY: Float = 0f
        var alignTop = Align.top
        var alignBottom = Align.bottom
    }

    private val alignTop
        get () = props.alignTop
    private val alignBottom
        get () = props.alignBottom



    override fun setPosition(x: Float, y: Float) {
        val actorProps = object {
            var x = 0f
            var y = 0f
            var width = 0f
            var height = 0f
        }

        val tooltipProps = object {
            var x = 0f
            var y = 0f
            var width = 0f
            var height = 0f
        }

        when (alignTop) {
            Align.top -> {
                tooltipProps.y = actorProps.y + actorProps.height
            }
            Align.bottom -> {
                tooltipProps.y = actorProps.y - tooltipProps.height
            }
            Align.left -> {
                tooltipProps.x = actorProps.x - tooltipProps.width
            }
            Align.right -> {
                tooltipProps.x = actorProps.x + actorProps.width
            }
        }

        when (alignBottom) {
            Align.top -> {
                tooltipProps.y = actorProps.y + actorProps.height
            }
            Align.bottom -> {
                tooltipProps.y = actorProps.y - tooltipProps.height
            }
            Align.left -> {
                tooltipProps.x = actorProps.x - tooltipProps.width
            }
            Align.right -> {
                tooltipProps.x = actorProps.x + actorProps.width
            }
        }

        super.setPosition(tooltipProps.x, tooltipProps.y)
    }

    fun <A:Actor> A.builder(actor: A, tooltipBuilder: Tooltip.Builder.() -> Unit) : Tooltip  {
        val builder = Tooltip.Builder(this)
        return builder.target(this).apply(tooltipBuilder).build()
    }




    companion object {
    }

}

