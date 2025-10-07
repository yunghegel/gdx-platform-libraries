package dev.jamiecrown.gdx.ui

import java.io.File

fun main()  {
    val dir = File("skin/skin_images")
    dir.listFiles()?.let { files ->
        println(files.size)
    }
}