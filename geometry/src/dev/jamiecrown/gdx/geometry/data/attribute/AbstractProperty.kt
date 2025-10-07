package dev.jamiecrown.gdx.geometry.data.attribute

import dev.jamiecrown.gdx.geometry.core.Indexed

abstract class AbstractProperty<T, V, TArr>(override val numComponents: Int, override val name: String) :
    Property<T, V, TArr> where T : Indexed {

    override var data: TArr? = null

}