package dev.jamiecrown.gdx.core.util

import kotlin.reflect.KClass

fun isSubclass(subclass: KClass<*>, superclass: KClass<*>): Boolean {
    return superclass.java.isAssignableFrom(subclass.java)
}

