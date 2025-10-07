package dev.jamiecrown.gdx.ui.scene2d

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import dev.jamiecrown.gdx.ui.ControlScale
import dev.jamiecrown.gdx.ui.sliderTable
import dev.jamiecrown.gdx.ui.table
import java.util.function.Consumer
import java.util.function.Supplier

class ColorPicker @JvmOverloads constructor(
    colorModel: Color,
    alpha: Boolean,
    skin: Skin,
    var callback: ((Color)->Unit)? = null
) : Table(skin) {
    private val colorModel: Color
    private val hsvModel = FloatArray(3)

    private enum class Range {
        LINEAR_UNIT, ANGLE_360
    }

    private inner class ColorVector(val slider: Slider, val getter: Supplier<Float>)

    private val colorVectors = Array<ColorVector>()
    private val colorPreview: Image
    private var alphaPreview: Image? = null

    init {
        defaults().pad(4f)
        this.colorModel = colorModel
        colorModel.toHsv(hsvModel)

        val tabPane = TabPane(skin, "toggle")

        tabPane.addPane("HSV", hsvPane())
        tabPane.addPane("RGB", rgbPane())
        tabPane.currentIndex = 0

        colorPreview = Image(skin, "white")
        colorPreview.setColor(colorModel.r, colorModel.g, colorModel.b, 1f)

        add(colorPreview).width(64f).fillY()
        add(tabPane)
        row()

        if (alpha) {
            alphaPreview = Image(skin, "white")
            alphaPreview?.setColor(colorModel.a, colorModel.a, colorModel.a, 1f)
            add(alphaPreview).width(64f).fill()
            add(alphaPane())
            row()
        }
    }

    private fun alphaPane(): Actor {
        val t = table(skin)
        addColorVector(
            t,
            "A",
            { colorModel.a },
            { v: Float? -> colorModel.a = v!! },
            { this.alphaChanged() },
            Range.LINEAR_UNIT
        )
        return t
    }

    private fun rgbPane(): Actor {
        val t = table(skin)
        addColorVector(
            t,
            "R",
            { colorModel.r },
            { v: Float? -> colorModel.r = v!! },
            { this.rgbChanged() },
            Range.LINEAR_UNIT
        )
        addColorVector(
            t,
            "G",
            { colorModel.g },
            { v: Float? -> colorModel.g = v!! },
            { this.rgbChanged() },
            Range.LINEAR_UNIT
        )
        addColorVector(
            t,
            "B",
            { colorModel.b },
            { v: Float? -> colorModel.b = v!! },
            { this.rgbChanged() },
            Range.LINEAR_UNIT
        )
        return t
    }

    private fun hsvPane(): Actor {
        val t = table(skin)
        addColorVector(t, "H", { hsvModel[0] }, { v: Float -> hsvModel[0] = v }, { this.hsvChanged() }, Range.ANGLE_360)
        addColorVector(
            t,
            "S",
            { hsvModel[1] },
            { v: Float -> hsvModel[1] = v },
            { this.hsvChanged() },
            Range.LINEAR_UNIT
        )
        addColorVector(
            t,
            "V",
            { hsvModel[2] },
            { v: Float -> hsvModel[2] = v },
            { this.hsvChanged() },
            Range.LINEAR_UNIT
        )
        return t
    }

    private fun rgbChanged() {
        colorModel.toHsv(hsvModel)
        updateColorVectors()
    }

    private fun hsvChanged() {
        colorModel.fromHsv(hsvModel)
        updateColorVectors()
    }

    private fun alphaChanged() {
        alphaPreview!!.setColor(colorModel.a, colorModel.a, colorModel.a, 1f)
        updateColorVectors()
    }

    private fun addColorVector(
        table: Table,
        name: String,
        getter: Supplier<Float>,
        setter: Consumer<Float>,
        updater: Runnable,
        range: Range
    ) {
        val container = Table()
        val slider = sliderTable(
            container,
            skin,

            100f,
            name,
            0f,
            if (range == Range.LINEAR_UNIT) 1f else 360f,
            0.01f,
            getter.get(),
            ControlScale.LIN
        ) {
            setter.accept(it)
            updater.run()
        }
        table.add(container).fillX().width(150f).row()
        colorVectors.add(ColorVector(slider, getter))
    }

    private fun updateColorVectors() {
        for (cv in colorVectors) {
            cv.slider.setProgrammaticChangeEvents(false)
            cv.slider.setValue(cv.getter.get())
            cv.slider.setProgrammaticChangeEvents(true)
        }
        colorPreview.setColor(colorModel.r, colorModel.g, colorModel.b, 1f)
        callback?.invoke(colorModel)
    }
}