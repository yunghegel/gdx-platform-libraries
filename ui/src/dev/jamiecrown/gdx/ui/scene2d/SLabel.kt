package dev.jamiecrown.gdx.ui.scene2d
import com.badlogic.gdx.scenes.scene2d.ui.Label
import dev.jamiecrown.gdx.ui.UI
open class SLabel(text: String, styleName: String = "default") : Label(text, UI.skin, styleName) {

    init {

    }

    private var provider: (() -> String)? = null

    var widthFunction : (() -> Float)? = null

    constructor(text: String, provider: () -> String) : this(text, "default") {
        this.provider = provider
    }

    override fun act(delta: Float) {
        if (provider != null) {
            setText(provider!!.invoke())
        }
        super.act(delta)
    }

    override fun getPrefWidth(): Float {
        return widthFunction?.invoke() ?: super.getPrefWidth()
    }

//    [#xxxxxxxx] Sets the color specified by the hex value xxxxxxxx in the form RRGGBBAA where AA is optional and defaults to 0xFF.
//      [] Sets the color to the previous color (kind of optional end tag)
//    f
    companion object {

    const val DEFAULT : String  = "default"
    const val DEFAULT_SMALL : String  = "default-small"
    const val DEFAULT_LARGE : String  = "default-large"
    const val DEFAULT_MEDIUM : String  = "default-medium"

    const val BOLD : String  = "bold"
    const val MONO : String  = "mono"

    const val INTER : String  = "inter"
    const val INTER_SMALL: String  = "inter-small"

    val styles = mapOf(
        "DEFAULT" to DEFAULT,
        "DEFAULT_SMALL" to DEFAULT_SMALL,
        "DEFAULT_LARGE" to DEFAULT_LARGE,
        "DEFAULT_MEDIUM" to DEFAULT_MEDIUM,
        "BOLD" to BOLD,
        "MONO" to MONO,
        "INTER" to INTER,
        "INTER_SMALL" to INTER_SMALL
    )


    fun replaceAnsiWithMarkup(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            if (text[i] == '[') {
                if (text[i + 1] == '#') {
                    val hex = text.substring(i + 2, i + 10)
                    val color = hex.toInt(16)
                    sb.append("[#${color.toString(16)}]")
                    i += 10
                } else {
                    sb.append("[#ffffff]")
                    i += 1
                }
            } else {
                sb.append(text[i])
                i += 1
            }
        }
        return sb.toString()
    }
    }



}
