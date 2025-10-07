package dev.jamiecrown.gdx.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Pools
import com.kotcrab.vis.ui.widget.MenuItem
import com.kotcrab.vis.ui.widget.PopupMenu
import com.kotcrab.vis.ui.widget.Tooltip
import com.ray3k.stripe.PopTable
import com.ray3k.stripe.PopTableClickListener
import dev.jamiecrown.gdx.core.Format
import dev.jamiecrown.gdx.core.Format.floatToString
import dev.jamiecrown.gdx.ui.scene2d.ColorBox
import dev.jamiecrown.gdx.ui.scene2d.STable
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.async.interval
import ktx.scene2d.Scene2DSkin
import kotlin.math.log10

const val CENTER = 1
const val TOP = 2
const val BOTTOM = 4
const val LEFT = 8
const val RIGHT = 16
const val TOP_LEFT = 10
const val TOP_RIGHT = 18
const val BOTTOM_LEFT = 12
const val BOTTOM_RIGHT = 20

fun <T: Actor> T.tip(conf: STable.() -> Unit) {
    val table = STable()
    table.conf()

    val tip = Tooltip.Builder(table).target(this).build()

}

fun Actor.sizeHashCode(): Int {
    return (width * 1000 + height).toInt()
}

fun Actor.getBounds(out:Rectangle) {
    out.set(x, y, width, height)
}

fun Actor.getBounds(): Rectangle {
    return Rectangle(x, y, width, height)
}

fun Cell<*>.padHorizontal(value: Float) : Cell<*> {
    padLeft(value)
    padRight(value)
    return this
}

fun Cell<*>.padVertical(value: Float) : Cell<*>{
    padTop(value)
    padBottom(value)
    return this
}

fun textureDrawable(texture:Texture) : TextureRegionDrawable {
    return TextureRegionDrawable(texture)
}

fun createColorPixel(color: Color): TextureRegion {
    val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    pixmap.setColor(color)
    pixmap.fill()
    return TextureRegion(Texture(pixmap))
}

fun ScrollPane.defaults() {
    setScrollingDisabled(true,false); setScrollbarsVisible(true); setOverscroll(false,false); setFlingTime(0f)
}

fun Table.clearbackground() {
    background = null
}
fun Skin.drawable(name:String, color : Color? = null) : Drawable {
    val drawable = getDrawable(name)
    if (drawable is NinePatchDrawable && color !=null) return drawable.tint(color)
    return drawable
}
enum class ControlScale {
    LIN, LOG
}

const val DEFAULT_PADDING = 4f

fun Actor.newPopupTable(uiStage:Stage, conf: PopTableClickListener.() -> Unit = {}): PopTable {
    val listener = PopTableClickListener(Scene2DSkin.defaultSkin)
    listener.popTable.isModal = false
    listener.popTable.isHideOnUnfocus = true


    listener.apply {
        conf()
    }
    addListener(listener)
    val table = listener.popTable
    table.attachToActor(this, Align.topLeft, Align.bottomLeft)


    onClick {


        if (listener.popTable.isVisible) {
            table.hide()
        } else if (stage != null)  {
            table.show(stage)
        }
    }

    return table
}

fun popupMenu(actor: Actor, conf: PopTable.() -> Unit) : PopTable {

    val table = PopTable(Scene2DSkin.defaultSkin)
    table.isHideOnUnfocus = true
    table.attachToActor(actor,Align.bottomRight,Align.bottomLeft)
    table.conf()

    val listener = object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) {
            if (table.isHidden) table.show(actor.stage)
            else table.hide()
        }
    }




    actor.addListener(listener)
    actor.onChange {
        if (table.isVisible) table.hide()
    }
    return table
}


fun PopTable.option(text: String, action: () -> Unit) {
    val menu = MenuItem(text)
    menu.onChange {
        action()
        hide()
    }
}

fun PopTableClickListener.table(conf: Table.() -> Unit) {
    popTable.conf()
}

fun Slider.onChangeComplete(complete: (Float) -> Unit) {
    onChange {
        if(!isDragging)  {
            complete(value)
        }
    }
}

fun label(text:String, style: String = "default",skin:Skin, supplier:(() -> String)?): Label {
    val label : Label
    label = if (supplier != null) {
        object : Label(supplier(),skin) {
            override fun act(delta: Float) {
                interval(1f) {
                    setText(supplier())
                }

                super.act(delta)
            }
        }
    } else {
        Label(text, skin)
    }
    return label
}

fun table(skin:Skin) :Table{
    return Table(skin)
}

fun slider(min: Float, max: Float, stepSize: Float, value: Float?,skin:Skin, change: (Float)->Unit={}, complete: (Float)->Unit={}): Slider {
    val slider = Slider(min, max, stepSize,false,skin)
    if (value != null) slider.setValue(value)
    slider.onChange {
        change(this.value)
    }
    slider.onChangeComplete {
        complete(slider.value)
    }
    return slider
}

fun slider(table:Table, skin:Skin = table.skin,width: Float = 100f, label: String = "", min: Float=0f, max: Float, step: Float, value: Float = max,row:Boolean = true, scale: ControlScale=ControlScale.LIN, callback: (Float) -> Unit): Slider {
    val stepSize = (max - min) / width

    val sMin = if (scale == ControlScale.LOG) log10(min.toDouble()).toFloat() else min
    val sMax = if (scale == ControlScale.LOG) log10(max.toDouble()).toFloat() else max
    val sStep = if (scale == ControlScale.LOG) log10(step.toDouble()).toFloat() else stepSize
    val sValue = if (scale == ControlScale.LOG) log10(value.toDouble()).toFloat() else value

    val num = Label(value.toString(),skin)
    val name = Label(label,skin)
    val slider = slider(sMin,sMax, sStep, sValue,skin) {
        val v = if (scale == ControlScale.LOG) Math.pow(10.0, it.toDouble()).toFloat() else it
        callback(v)
        num.setText(v.toString())
    }
    if (row) {
        table.row()
    }
    table.add(name).pad(DEFAULT_PADDING)
    table.add(slider).width(width).pad(DEFAULT_PADDING)
    table.add(num).pad(DEFAULT_PADDING)
    return slider
}

fun slideri(skin:Skin,min: Int, max: Int, stepSize: Int, value: Int?, change: (Int)->Unit={}, complete: (Int)->Unit={}): Slider {
    val slider = Slider(min.toFloat(), max.toFloat(), stepSize.toFloat(),false,skin)
    if (value != null) slider.setValue(value.toFloat())
    slider.onChange {
        change(this.value.toInt())
    }
    slider.onChangeComplete {
        complete(slider.value.toInt())
    }
    return slider
}

fun sliderTable(
    table: Table = Table(),
    skin: Skin = table.skin,
    width: Float = 100f,
    label: String,
    min: Float,
    max: Float,
    step: Float,
    value: Float,
    scale: ControlScale,
    callback: (Float) -> Unit
): Slider {
    val stepSize = (max - min) / width

    val sMin = if (scale == ControlScale.LOG) log10(min.toDouble()).toFloat() else min
    val sMax = if (scale == ControlScale.LOG) log10(max.toDouble()).toFloat() else max
    val sStep = if (scale == ControlScale.LOG) log10(step.toDouble()).toFloat() else stepSize
    val sValue = if (scale == ControlScale.LOG) log10(value.toDouble()).toFloat() else value


    val num = object : Label(value.toString(),skin) {
        override fun setText(newText: CharSequence?) {
            super.setText(Format.trimFloat(newText.toString().toFloat(),2))
        }
    }
    val name = Label(label,skin)
    val slider = slider(sMin, sMax, sStep, sValue,skin) {
        val v = if (scale == ControlScale.LOG) Math.pow(10.0, it.toDouble()).toFloat() else it
        callback(v)
        num.setText(v.toString())
    }
    val container = Table()
    with(container) {
        add(name).pad(DEFAULT_PADDING)
        add(slider).width(width).pad(DEFAULT_PADDING)
        add(num).pad(DEFAULT_PADDING).width(25f)
    }
    table.add(container).pad(DEFAULT_PADDING).width(width).row()
    return slider
}

fun sliderTablei(
    table: Table = Table(),
    skin:Skin = table.skin,
    width: Float = 100f,
    label: String,
    min: Int,
    max: Int,
    step: Int,
    value: Int,
    scale: ControlScale,
    callback: (Int) -> Unit
): Table {
    val stepSize = (max - min) / width

    val sMin = if (scale == ControlScale.LOG) log10(min.toDouble()).toFloat() else min.toFloat()
    val sMax = if (scale == ControlScale.LOG) log10(max.toDouble()).toFloat() else max.toFloat()
    val sStep = if (scale == ControlScale.LOG) log10(step.toDouble()).toFloat() else stepSize.toFloat()
    val sValue = if (scale == ControlScale.LOG) log10(value.toDouble()).toFloat() else value.toFloat()

    val num =object : Label(value.toString(), skin) {
        override fun act(delta: Float) {
            setText(floatToString(value.toFloat(),2))
            super.act(delta)
        }
    }
    val name = Label(label,skin)
    val slider = slideri(skin,min, max, step, value) {
        val v = if (scale == ControlScale.LOG) Math.pow(10.0, it.toDouble()).toInt() else it
        callback(v)
        num.setText(v.toString())
    }
    val container = Table()
    container.add(name).pad(DEFAULT_PADDING)
    container.add(slider).pad(DEFAULT_PADDING)
    container.add(num).pad(DEFAULT_PADDING)
    table.add(container).pad(DEFAULT_PADDING).width(width).row()
    return table
}

fun toggle(table: Table, label: String, value: Boolean,row:Boolean = false, callback: (Boolean) -> Unit): CheckBox {
    val check = CheckBox(label,table.skin )
    check.isChecked = value
    check.onChange {
        callback(check.isChecked)
    }
    table.add(check).pad(DEFAULT_PADDING)
    if(row) {
        table.row()
    }
    return check
}
fun colorBox(table: Table, name: String, colorModel: Color, alpha: Boolean,skin:Skin=table.skin): Cell<*> {
    val t = Table()
    t.skin = skin
    t.add(name)
    t.add(ColorBox(name, { colorModel }, alpha,skin))
    val cell: Cell<*> = table.add(t).left()
    table.row()
    return cell
}

fun colorBox(table: Table, colorModel: Color, alpha: Boolean,skin:Skin=table.skin): Cell<*> {
    val t = Table()
    t.skin = skin

    t.add(ColorBox("", { colorModel }, alpha,skin))
    val cell: Cell<*> = table.add(t).left()
    table.row()
    return cell
}

val Actor.dimensions : Pair<Float,Float> get() = Pair(width,height)

fun colorBox(
    table: Table,
    name: String,
    colorModel: Color?,
    alpha: Boolean,
    skin:Skin = table.skin,
    row: Boolean
): Cell<*> {
    val t = Table()
    t.add(name)
    t.add(ColorBox(name, { colorModel!! }, alpha,skin ))
    if (row) {
        val cell: Cell<*> = table.add(t).left()
        table.row()
        return cell
    }
    return table.add(t).left()
}

fun dialog(content: Actor, title: String?,skin:Skin): Dialog {
    val dialog = Dialog(title, skin, "dialog")
    dialog.contentTable.add(content).row()
    // dialog.getContentVisTable().add(trig(skin, "Close", ()->dialog.hide()));
    dialog.button("Close", dialog)

    return dialog
}

fun recurseTable( table: Table, task: (Actor)->Unit) {
    for (child in table.children) {
        task(child)
        if (child is Table) {
            recurseTable(child, task)
        }
    }
}

fun cancelAllInputFocus(stage: Stage) {
    stage.actors.forEach { actor ->
        val focusevent = Pools.obtain(InputEvent::class.java)
        focusevent.type = InputEvent.Type.touchUp
        val mouse =  stage.screenToStageCoordinates(Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
        focusevent.stageX = mouse.x
        focusevent.stageY = mouse.y
        actor.fire(focusevent)
        stage.cancelTouchFocus(actor)
        Pools.free(focusevent)
    }
    stage.setKeyboardFocus(null)
    stage.setScrollFocus(null)
}

fun PopupMenu.newItem(text:String,action: ()->Unit){
    val menu = MenuItem(text)
    menu.onChange {
        action()
    }
}

private val tempVec = Vector2()
fun highlightActor(
    actor: Actor,
    stage: Stage,
    shapeRenderer: ShapeRenderer,
    color: Color = Color.WHITE,
    lineWidth: Float = 2f
) {
    // Actor needs to be part of a stage to get coordinates
    if (actor.stage == null) {
        // Gdx.app.log("highlightActor", "Actor is not added to a stage.") // Optional logging
        return
    }

    val width = actor.width
    val height = actor.height

    // Calculate the four corners in stage coordinates
    // tempVec is reused to avoid garbage collection
    val bottomLeft = actor.localToStageCoordinates(tempVec.set(0f, 0f))
    val bottomRight = actor.localToStageCoordinates(tempVec.set(width, 0f))
    val topRight = actor.localToStageCoordinates(tempVec.set(width, height))
    val topLeft = actor.localToStageCoordinates(tempVec.set(0f, height))

    // Store previous line width and set the new one
    // Note: glLineWidth might not be supported on all platforms (e.g., WebGL 1)
    // and might have limitations on thickness.
    // For thicker/more reliable lines, consider drawing filled polygons or multiple thin lines.
    val previousLineWidth = 1f
    Gdx.gl.glLineWidth(lineWidth)

    // Set the projection matrix for the ShapeRenderer
    shapeRenderer.projectionMatrix = stage.camera.combined

    // Begin drawing lines
    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
    shapeRenderer.color = color

    // Draw the rectangle outline
    shapeRenderer.line(bottomLeft, bottomRight)
    shapeRenderer.line(bottomRight, topRight)
    shapeRenderer.line(topRight, topLeft)
    shapeRenderer.line(topLeft, bottomLeft)

    // End drawing
    shapeRenderer.end()

    // Reset line width to default (or the stored previous value if necessary)
    Gdx.gl.glLineWidth(1f) // Reset to default common value
}