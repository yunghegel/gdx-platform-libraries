package dev.jamiecrown.gdx.core

const val ANSI_RESET: String = "\u001B[0m"
const val ANSI_BLACK: String = "\u001B[30m"
const val ANSI_RED: String = "\u001B[31m"
const val ANSI_GREEN: String = "\u001B[32m"
const val ANSI_YELLOW: String = "\u001B[33m"
const val ANSI_BLUE: String = "\u001B[34m"
const val ANSI_PURPLE: String = "\u001B[35m"
const val ANSI_CYAN: String = "\u001B[36m"
const val ANSI_WHITE: String = "\u001B[37m"

enum class Ansi(val ansi: String, val markup: String) {
    BLACK(ANSI_BLACK,"[BLACK]"),
    RED(ANSI_RED,"[RED]"),
    GREEN(ANSI_GREEN,"[FOREST]"),
    YELLOW(ANSI_YELLOW,"[YELLOW]"),
    BLUE(ANSI_BLUE,"[SKY]"),
    PURPLE(ANSI_PURPLE,"[VIOLET]"),
    CYAN(ANSI_CYAN,"[TEAL]"),
    WHITE(ANSI_WHITE,"[WHITE]"),
    RESET(ANSI_RESET,"[]");

    fun colorize(text: String): String {
        return "$ansi$text$ANSI_RESET"
    }

    companion object {
        fun replaceAnsi(text: String): String {
            var result = text
            for (ansi in entries) {
                result = result.replace(ansi.ansi, ansi.markup)
            }
            return result
        }
    }



}

fun String.green(): String {
    return Ansi.GREEN.colorize(this)
}

fun String.red(): String {
    return Ansi.RED.colorize(this)
}

fun String.yellow(): String {
    return Ansi.YELLOW.colorize(this)
}

fun String.blue(): String {
    return Ansi.BLUE.colorize(this)
}

fun String.purple(): String {
    return Ansi.PURPLE.colorize(this)
}

fun String.cyan(): String {
    return Ansi.CYAN.colorize(this)
}

fun String.white(): String {
    return Ansi.WHITE.colorize(this)
}