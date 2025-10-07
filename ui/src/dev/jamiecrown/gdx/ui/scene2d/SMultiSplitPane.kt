package dev.jamiecrown.gdx.ui.scene2d

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.FloatArray
import com.badlogic.gdx.utils.SnapshotArray
import com.kotcrab.vis.ui.FocusManager
import com.kotcrab.vis.ui.widget.MultiSplitPane
import com.kotcrab.vis.ui.widget.internal.SplitPaneCursorManager
import dev.jamiecrown.gdx.ui.UI
import dev.jamiecrown.gdx.ui.widget.CollapseActor
import java.util.*
import kotlin.math.max
import kotlin.math.min


open class SMultiSplitPane(vertical: Boolean = false, style: MultiSplitPane.MultiSplitPaneStyle = UI.skin.get("default",
    MultiSplitPane.MultiSplitPaneStyle::class.java)) : WidgetGroup() {
    private var style: MultiSplitPane.MultiSplitPaneStyle = style
    var vertical = false

    val widgetBounds = Array<Rectangle>()
    val scissors = Array<Rectangle>()

    val handleBounds = Array<Rectangle>()
    val splits: FloatArray = com.badlogic.gdx.utils.FloatArray()

    private var handlePosition = Vector2()
    private var lastPoint = Vector2()

    private var handleOver: Rectangle? = null
    private var handleOverIndex = 0

    private var widgetCount = 0

    init {
        this.vertical = vertical
        setStyle(style)
        setSize(getPrefWidth(), getPrefHeight())
        initialize()

    }


    private fun initialize() {
        addListener(object : SplitPaneCursorManager(this, vertical) {
            override fun handleBoundsContains(x: Float, y: Float): Boolean {
                return getHandleContaining(x, y) != null
            }

            override fun contains(x: Float, y: Float): Boolean {
                for (bound in widgetBounds) {
                    if (bound.contains(x, y)) return true
                }
                return getHandleContaining(x, y) != null
            }
        })

        addListener(object : InputListener() {
            var draggingPointer: Int = -1

            override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                if (isTouchable() == false) return false

                if (draggingPointer != -1) return false
                if (pointer == 0 && button != 0) return false
                val containingHandle: Rectangle? = getHandleContaining(x, y)
                if (containingHandle != null) {
                    handleOverIndex = handleBounds.indexOf(containingHandle, true)
                    FocusManager.resetFocus(getStage())

                    draggingPointer = pointer
                    lastPoint[x] = y
                    handlePosition[containingHandle.x] = containingHandle.y
                    return true
                }
                return false
            }

            override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
                if (pointer == draggingPointer) draggingPointer = -1
                handleOver = getHandleContaining(x, y)
            }

            override fun mouseMoved(event: InputEvent, x: Float, y: Float): Boolean {
                handleOver = getHandleContaining(x, y)
                return false
            }

            override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
                if (pointer != draggingPointer) return

                val handle: Drawable = style!!.handle
                if (!vertical) {
                    val delta = x - lastPoint.x
                    val availWidth: Float = getWidth() - handle.minWidth
                    var dragX = handlePosition.x + delta
                    handlePosition.x = dragX
                    dragX = max(0.0, dragX.toDouble()).toFloat()
                    dragX = min(availWidth.toDouble(), dragX.toDouble()).toFloat()
                    val targetSplit = dragX / availWidth
                    setSplit(handleOverIndex, targetSplit)
                    lastPoint[x] = y
                } else {
                    val delta = y - lastPoint.y
                    val availHeight: Float = getHeight() - handle.minHeight
                    var dragY = handlePosition.y + delta
                    handlePosition.y = dragY
                    dragY = max(0.0, dragY.toDouble()).toFloat()
                    dragY = min(availHeight.toDouble(), dragY.toDouble()).toFloat()
                    val targetSplit = 1 - (dragY / availHeight)
                    setSplit(handleOverIndex, targetSplit)
                    lastPoint[x] = y
                }
                invalidate()
            }
        })
    }

    private fun getHandleContaining(x: Float, y: Float): Rectangle? {
        for (rect in handleBounds) {
            if (rect.contains(x, y)) {
                return rect
            }
        }

        return null
    }

    /**
     * Returns the split pane's style. Modifying the returned style may not have an effect until [.setStyle]
     * is called.
     */
    fun getStyle(): MultiSplitPane.MultiSplitPaneStyle {
        return style
    }

    fun setStyle(style: MultiSplitPane.MultiSplitPaneStyle) {
        this.style = style
        invalidateHierarchy()
    }

    override fun layout() {
        if (!vertical) calculateHorizBoundsAndPositions()
        else calculateVertBoundsAndPositions()

        val actors: SnapshotArray<Actor> = getChildren()
        for (i in 0 until actors.filter{ it !is CollapseActor}.size ) {
            val actor = actors[i]
            val bounds = widgetBounds[i]
            actor.setBounds(bounds.x, bounds.y, bounds.width, bounds.height)
            if (actor is Layout) (actor as Layout).validate()
        }
    }

    override fun getPrefWidth(): Float {
        var width = 0f
        for (actor in getChildren()) {
            width = if (actor is Layout) (actor as Layout).prefWidth else actor.width
        }
        if (!vertical) width += handleBounds.size * style!!.handle.getMinWidth()
        return width
    }

    override fun getPrefHeight(): Float {
        var height = 0f
        for (actor in getChildren()) {
            height = if (actor is Layout) (actor as Layout).prefHeight else actor.height
        }
        if (vertical) height += handleBounds.size * style.handle.getMinHeight()
        return height
    }

    override fun getMinWidth(): Float {
        return 0f
    }

    override fun getMinHeight(): Float {
        return 0f
    }



    private fun calculateHorizBoundsAndPositions() {
        val height: Float = getHeight()
        val width: Float = getWidth()
        val handleWidth: Float = style.handle.getMinWidth()

        val availWidth = width - (handleBounds.size * handleWidth)

        var areaUsed = 0f
        var currentX = 0f
        for (i in 0 until splits.size) {
            val areaWidthFromLeft = (availWidth * splits[i]).toInt().toFloat()
            val areaWidth = areaWidthFromLeft - areaUsed
            areaUsed += areaWidth
            widgetBounds[i][currentX, 0f, areaWidth] = height
            currentX += areaWidth
            handleBounds[i][currentX, 0f, handleWidth] = height
            currentX += handleWidth
        }
        if (widgetBounds.size != 0) widgetBounds.peek()[currentX, 0f, availWidth - areaUsed] = height
    }

    private fun calculateVertBoundsAndPositions() {
        val width: Float = getWidth()
        val height: Float = getHeight()
        val handleHeight: Float = style.handle.getMinHeight()

        val availHeight = height - (handleBounds.size * handleHeight)

        var areaUsed = 0f
        var currentY = height
        for (i in 0 until splits.size) {
            val areaHeightFromTop = (availHeight * splits[i]).toInt().toFloat()
            val areaHeight = areaHeightFromTop - areaUsed
            areaUsed += areaHeight
            widgetBounds[i][0f, currentY - areaHeight, width] = areaHeight
            currentY -= areaHeight
            handleBounds[i][0f, currentY - handleHeight, width] = handleHeight
            currentY -= handleHeight
        }
        if (widgetBounds.size != 0) widgetBounds.peek()[0f, 0f, width] = availHeight - areaUsed
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        validate()

        val color: Color = getColor()

        applyTransform(batch, computeTransform())

        val actors: SnapshotArray<Actor> = getChildren()
        for (i in 0 until actors.size) {
            val actor = actors[i]
            val bounds = widgetBounds[i]
            val scissor = scissors[i]
            getStage().calculateScissors(bounds, scissor)
            if (ScissorStack.pushScissors(scissor)) {
                if (actor.isVisible) actor.draw(batch, parentAlpha * color.a)
                batch.flush()
                ScissorStack.popScissors()
            }
        }

        batch.setColor(color.r, color.g, color.b, parentAlpha * color.a)

        val handle: Drawable = style.handle
        var handleOver: Drawable = style.handle
        if (isTouchable() && style.handleOver != null) handleOver = style.handleOver
        for (rect in handleBounds) {
            if (this.handleOver === rect) {
                handleOver.draw(batch, rect.x, rect.y, rect.width, rect.height)
            } else {
                handle.draw(batch, rect.x, rect.y, rect.width, rect.height)
            }
        }
        resetTransform(batch)
    }

    override fun hit(x: Float, y: Float, touchable: Boolean): Actor? {
        if (touchable && getTouchable() == Touchable.disabled) return null
        return if (getHandleContaining(x, y) != null) {
            this
        } else {
            super.hit(x, y, touchable)
        }
    }

    /** Changes widgets of this split pane. You can pass any number of actors even 1 or 0. Actors can't be null.  */
    open fun setWidgets(vararg actors: Actor) {
        setWidgets(Arrays.asList(*actors))
    }

    /** Changes widgets of this split pane. You can pass any number of actors even 1 or 0. Actors can't be null.  */
    open fun setWidgets(actors: Iterable<Actor?>) {
        clearChildren()
        widgetBounds.clear()
        scissors.clear()
        handleBounds.clear()
        splits.clear()
        widgetCount = actors.count()

        for (actor in actors) {
            super.addActor(actor)
            widgetBounds.add(Rectangle())
            scissors.add(Rectangle())
        }
        var currentSplit = 0f
        val splitAdvance: Float = 1f / getChildren().size
        for (i in 0 until getChildren().size - 1) {
            handleBounds.add(Rectangle())
            currentSplit += splitAdvance
            splits.add(currentSplit)
        }
        invalidate()
    }

    /**
     * @param handleBarIndex index of handle bar starting from zero, max index is number of widgets - 1
     * @param split new value of split, must be greater than 0 and lesser than 1 and must be smaller and bigger than
     * previous and next split value. Invalid values will be clamped to closest valid one.
     */
    open fun setSplit(handleBarIndex: Int, split: Float) {
        var split = split
        check(handleBarIndex >= 0) { "handleBarIndex can't be < 0" }
        check(handleBarIndex < splits.size) { "handleBarIndex can't be >= splits size" }
        val minSplit = if (handleBarIndex == 0) 0f else splits[handleBarIndex - 1]
        val maxSplit = if (handleBarIndex == splits.size - 1) 1f else splits[handleBarIndex + 1]
        split = MathUtils.clamp(split, minSplit, maxSplit)
        splits[handleBarIndex] = split
    }

    override fun addActorAfter(actorAfter: Actor?, actor: Actor?) {
        throw UnsupportedOperationException("Use MultiSplitPane#setWidgets")
    }

//    override fun addActor(actor: Actor?) {
//        throw UnsupportedOperationException("Use MultiSplitPane#setWidgets")
//    }

    override fun addActorAt(index: Int, actor: Actor?) {
        throw UnsupportedOperationException("Use MultiSplitPane#setWidgets")
    }

    override fun addActorBefore(actorBefore: Actor?, actor: Actor?) {
        throw UnsupportedOperationException("Use MultiSplitPane#setWidgets")
    }

    override fun removeActor(actor: Actor?): Boolean {
        throw UnsupportedOperationException("Use MultiSplitPane#setWidgets")
    }


}