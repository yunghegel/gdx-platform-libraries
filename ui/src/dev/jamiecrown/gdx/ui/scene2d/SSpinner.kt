package dev.jamiecrown.gdx.ui.scene2d
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.kotcrab.vis.ui.widget.spinner.FloatSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.Spinner
import com.kotcrab.vis.ui.widget.spinner.SpinnerModel

class SSpinner(name: String, model: SpinnerModel):Spinner(name, model) {

    var onChange: (Number)->Unit = {}

    constructor(name: String, min: Float, max: Float, value: Float, step: Float):this(
        name,
        FloatSpinnerModel("$value", "$min", "$max", "$step")
                                                                                     )

    constructor(name: String, min: Int, max: Int, value: Int, step: Int):this(
        name,
        FloatSpinnerModel("$value", "$min", "$max", "$step")
                                                                             )

    init {
        getCell(textField).maxWidth(Defaults.maximumCellWidth)

        addListener(object:ChangeListener() {
            override fun changed(event: ChangeEvent, actor: com.badlogic.gdx.scenes.scene2d.Actor) {
                onChange((model as FloatSpinnerModel).value)
            }
        })
    }

    object Defaults {

        var maximumCellWidth = 75f
    }

}
