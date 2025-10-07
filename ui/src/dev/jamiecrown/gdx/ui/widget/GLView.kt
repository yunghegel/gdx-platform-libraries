package dev.jamiecrown.gdx.ui.widget

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.utils.viewport.ScreenViewport

class GLView(val viewport: ScreenViewport) : WidgetGroup() {

    var viewportOriginalY = 0
    var viewportOriginalX = 0

    val temp = Vector2()

    var bounds = Rectangle()

    init {
        touchable = Touchable.disabled
    }

    fun updateViewport(centerCamera: Boolean) {
        temp[MathUtils.round(width).toFloat()] = MathUtils.round(height).toFloat()
        if (stage != null) stage.viewport.project(temp)
        viewport.update(MathUtils.round(temp.x).toInt(), MathUtils.round(temp.y).toInt(), centerCamera)
        val viewportOriginalX = viewport.screenX
        val viewportOriginalY = viewport.screenY
        temp[0f] = 0f
        localToStageCoordinates(temp)
        viewport.setScreenPosition(
            (viewportOriginalX + MathUtils.round(temp.x)).toInt(),
            (viewportOriginalY + MathUtils.round(Gdx.graphics.height - temp.y)).toInt()
        )
        viewport.setWorldSize(width * viewport.unitsPerPixel, height * viewport.unitsPerPixel)
        viewport.setScreenBounds(
            temp.x.toInt(),
            temp.y.toInt(),
            width.toInt(),
            height.toInt()
        )
        bounds.set(temp.x, Gdx.graphics.height - temp.y, width, height)
        viewport.apply(centerCamera)

    }

    override fun layout() {
        temp[0f] = 0f
        localToScreenCoordinates(temp)
        if (viewport == null) return
//        viewport.update(com.badlogic.gdx.math.MathUtils.round(width), com.badlogic.gdx.math.MathUtils.round(height))
        viewportOriginalX = viewport.screenX
        viewportOriginalY = viewport.screenY
    }


    companion object {
        private val temp = Vector2()
    }

}