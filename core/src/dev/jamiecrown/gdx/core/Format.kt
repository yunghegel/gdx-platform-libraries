package dev.jamiecrown.gdx.core

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

object Format {
    fun formatVector3(v: Vector3): String {
        return trimFloat(v.x) + ", " + trimFloat(v.y) + ", " + trimFloat(v.z)
    }

    fun formatVector3(v: Vector3, n: Int): String {
        return floatToString(v.x, n) + ", " + floatToString(v.y, n) + ", " + floatToString(v.z, n)
    }

    fun formatVector2(v: Vector2): String {
        return trimFloat(v.x) + ", " + trimFloat(v.y)
    }

    fun formatVector2(v: Vector2, n: Int): String {
        return floatToString(v.x, n) + ", " + floatToString(v.y, n)
    }

    fun formatMatrix4(matrix4: Matrix4): String {
        //format matrix 4 with trimmed floats

        val values = matrix4.values
        val `val` = arrayOfNulls<String>(values.size)
        for (i in values.indices) {
            `val`[i] = trimFloat(values[i])
        }
        //now format into 4 rows of 4 columns
        return """
          [${`val`[0]}|${`val`[1]}|${`val`[2]}|${`val`[3]}]
          [${`val`[4]}|${`val`[5]}|${`val`[6]}|${`val`[7]}]
          [${`val`[8]}|${`val`[9]}|${`val`[10]}|${`val`[11]}]
          [${`val`[12]}|${`val`[13]}|${`val`[14]}|${`val`[15]}]
          
          """.trimIndent()
    }

    //null safe trim float to 2 decimal places
    fun trimFloat(f: Float,n:Int = 2): String {
        return if (f.isNaN()) "NaN" else floatToString(f,n)
    }

    fun floatToString(d: Float): String {
        //round to four decimal places //TODO: editor setting for choosing rounding precession
        var d = d
        d = Math.round(d * 10000).toFloat()
        d = d / 10000
        val s = d.toString()

        return s
    }

    //float to string with n decimal places
    fun floatToString(d: Float, n: Int): String {
        //round to four decimal places //TODO: editor setting for choosing rounding precession
        var d = d
        d = Math.round(d * Math.pow(10.0, n.toDouble())).toFloat()
        d = (d / Math.pow(10.0, n.toDouble())).toFloat()
        val s = d.toString()

        return s
    }

    fun humanBytes(value: Double): String {
        var value = value
        var suffix = " B"
        if (value > 1024) {
            value /= 1024.0
            suffix = " KB"
        }
        if (value > 1024) {
            value /= 1024.0
            suffix = " MB"
        }
        if (value > 1024) {
            value /= 1024.0
            suffix = " GB"
        }
        if (value > 1024) {
            value /= 1024.0
            suffix = " TB"
        }
        return (Math.round(value * 100.0) / 100.0).toString() + suffix
    }

//    sanitize a string for being parsed into a primitive value, ie. no empty space, etc
    fun sanitizePrimitiveString(str: String): String {
        return str.replace(" ", "").trim()
    }

    /**
     * Convert a camel case string to underscored capitalized string.
     * eg. thisIsStandardCamelCaseString is converted to THIS_IS_STANDARD_CAMEL_CASE_STRING
     * @param camelCase a camel case string : only letters without consecutive uppercase letters.
     * @return the transformed string or the same if not camel case.
     */
    fun camelCaseToUnderScoreUpperCase(camelCase: String): String {
        var result = ""
        var prevUpperCase = false
        for (i in 0 until camelCase.length) {
            val c = camelCase[i]
            if (!Character.isLetter(c)) return camelCase
            if (Character.isUpperCase(c)) {
                if (prevUpperCase) return camelCase
                result += "_$c"
                prevUpperCase = true
            } else {
                result += c.uppercaseChar()
                prevUpperCase = false
            }
        }
        return result
    }

    /**
     * Add spaces to a camel case string to make it more readable.
     */
    fun camelCaseToReadableFormat(camelCase: String,capitalizeFirst:Boolean =true): String {
        var result = ""
        var prevUpperCase = false
        for (i in 0 until camelCase.length) {
            val c = camelCase[i]
            if (!Character.isLetter(c)) return camelCase
            if (Character.isUpperCase(c) && !prevUpperCase) {
                if (i > 0) result += " "
                result += c
                prevUpperCase = true
            } else {
                result += c
                prevUpperCase = false
            }
        }

        if(capitalizeFirst) {
            result = result.substring(0, 1).uppercase() + result.substring(1)
        }

        return result
    }

    fun formatFieldName(name: String): String {
        var formatted = name
        if (isCamelCase(name)) {
            formatted = camelCaseToReadableFormat(name)
        } else
            if(isSnakeCase(name)) {
                formatted = snakeCaseToHumanReadable(name)
            }
        formatted = formatted.replace("_", " ")
        formatted = capitalizeFirst(formatted)

        return "$formatted:"
    }

    fun isCamelCase(str: String): Boolean {
        var prevUpperCase = false
        for (i in 0 until str.length) {
            val c = str[i]
            if (!Character.isLetter(c)) return false
            if (Character.isUpperCase(c) && prevUpperCase) return false
            prevUpperCase = Character.isUpperCase(c)
        }
        return true
    }

    fun isSnakeCase(str: String): Boolean {
        for (i in 0 until str.length) {
            val c = str[i]
            if (!Character.isLetterOrDigit(c) && c != '_') return false
        }
        return true
    }



    fun capitalizeFirst(str: String): String {
        return str.substring(0, 1).uppercase() + str.substring(1)
    }

    fun pascalCaseToHumanReadable(str: String): String {
        val sb = StringBuilder()
        var lastChar = ' '
        str.forEach {
            if (it.isUpperCase() && !lastChar.isUpperCase()) {
                sb.append(" ")
            } else {
                it.lowercase()
            }
            if (it != '_') {
                sb.append(it)
            }
            lastChar = it
        }
        return sb.toString()
    }

    fun snakeCaseToHumanReadable(str: String): String {
        val sb = StringBuilder()
        str.forEach {
            if (it == '_') {
                sb.append(" ")
            } else {
                sb.append(it)
            }
        }
        return sb.toString()
    }

    fun append(str: String, append: String): String {
        return if (str.isEmpty()) append else "$str$append"
    }

    fun append(str: String, append: String, separator: String= " "): String {
        return if (str.isEmpty()) append else "$str$separator$append"
    }

    fun prepend(str: String, prepend: String): String {
        return if (str.isEmpty()) prepend else "$prepend$str"
    }

    fun prepend(str: String, prepend: String, separator: String= " "): String {
        return if (str.isEmpty()) prepend else "$prepend$separator$str"
    }

    fun wrap(left: String, right: String, str: String): String {
        return "$left$str$right"
    }
}

class Printer(private val type: Class<*>, vararg labels: Label) {

    class Label (private val text: String?, private val value: Any?) {

        override fun toString(): String {
            var newValue = value
            if (newValue == null) newValue = EMPTY
            if (text == null || text.isEmpty()) return newValue.toString()
            //            return text + String.format(SEPERATOR, newValue);
            //String.format() is incompatible with GWT
            return "$text: $newValue"
        }

        companion object {
            private const val SEPERATOR = ": %s"
            private const val EMPTY = "Empty"
        }
    }

    private val labels: Array<Label>

    constructor(type: Class<*>) : this(type, (null as Label?)!!)

    init {
        this.labels = labels as Array<Label>
    }

    fun objectName(): String {
        return type.simpleName
    }

    override fun toString(): String {
        val nester = objectName() + NESTER
        var inner = ""
        for (pair in labels) inner += pair.toString() + SEPERATOR
        inner = inner.substring(0, inner.length - 2)
        //        return String.format(nester, inner);
        //String.format() is incompatible with GWT
        return "[$inner]"
    }

    companion object {
        private const val SEPERATOR = ", "
        private const val NESTER = "[%s]"
    }
}
