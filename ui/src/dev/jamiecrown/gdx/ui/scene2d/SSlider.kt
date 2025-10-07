package dev.jamiecrown.gdx.ui.scene2d
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import dev.jamiecrown.gdx.ui.UI
class SSlider(min: Float, max: Float, value: Float, stepSize: Float, vertical: Boolean) : Slider(
    min, max, stepSize, vertical,
    UI.skin
) {

    constructor(min: Float, max: Float, value: Float) : this(
        min,
        max,
        value,
        calcStepSize(Defaults.numSteps, min, max),
        false
    )

    constructor(value: Float, range: Float) : this(
        value + range, value - range, value,
        calcStepSize(Defaults.numSteps, 0f, range), false
    )

    constructor(min: Float, max: Float, value: Float, setter: (Float) -> Unit) : this(
        min, value,
        calcStepSize(Defaults.numSteps, min, max)
    ) {
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                setter(value)
            }
        })
    }

    constructor(value: Float, setter: (Float) -> Unit) : this(
        value - (value * 2), value + (value * 2), value,
        calcStepSize(Defaults.numSteps, value - (value * 2), value + (value * 2)), false
    ) {
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                setter(value)
            }
        })
    }

    object Defaults {

        var numSteps = 100

    }

    companion object {

        fun calcStepSize(numSteps: Int, minValue: Float, maxValue: Float): Float {
            return (maxValue - minValue) / numSteps
        }

        fun calcDefaultValue(minValue: Float, maxValue: Float): Float {
            return (maxValue - minValue) / 2
        }
    }

}
