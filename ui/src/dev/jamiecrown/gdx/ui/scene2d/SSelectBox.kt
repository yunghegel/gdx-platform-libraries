package dev.jamiecrown.gdx.ui.scene2d
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import dev.jamiecrown.gdx.ui.UI
open class SSelectBox<T>() : SelectBox<T>(UI.skin) {

    val names : MutableList<String> = mutableListOf()

    var nameMap : (T) -> String = { it.toString() }

    constructor(nameMap: (T) -> String) : this() {
        this.nameMap = nameMap
    }

    override fun toString(item: T): String {
        return nameMap(item)
    }

}