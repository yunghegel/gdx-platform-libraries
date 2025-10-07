package dev.jamiecrown.gdx.ui.scene2d
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import ktx.actors.onChange
import dev.jamiecrown.gdx.ui.UI
class STextButton(text: String, styleName: String = "default") : TextButton(text, UI.skin, styleName) {
    constructor(text: String) : this(text, "default")
    constructor(text:String,onChange:()->Unit) : this(text) {
        onChange { onChange() }
    }
    companion object {
        const val DEFAULT = "default"
        const val COMPACT = "compact"
        const val BOLD = "bold"
        const val LARGE = "large"
        const val MENU = "menu"
        const val ROUND_BLUE = "round-blue"
        const val TOGGLE = "toggle"
        const val SOFT = "soft"
        const val SOFT_BLUE = "soft-blue"
        const val EMPTY = "empty"
        const val TAB = "tab"

        val styles = mapOf(
            "DEFAULT" to DEFAULT,
            "COMPACT" to COMPACT,
            "BOLD" to BOLD,
            "LARGE" to LARGE,
            "MENU" to MENU,
            "ROUND_BLUE" to ROUND_BLUE,
            "TOGGLE" to TOGGLE,
            "SOFT" to SOFT,
            "SOFT_BLUE" to SOFT_BLUE,
            "EMPTY" to EMPTY,
            "TAB" to TAB
        )

    }
}


