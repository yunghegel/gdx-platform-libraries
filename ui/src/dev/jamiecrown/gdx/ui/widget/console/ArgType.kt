package dev.jamiecrown.gdx.ui.widget.console

enum class ArgType {

    STRING,
    INT,
    FLOAT,
    BOOL,
    TUPLE;

    fun read(input : String) : Result<Any> {
        return when (this) {
            STRING -> Result.success(input)
            INT -> try {
                Result.success(input.toInt())
            } catch (e: Exception) {
                Result.success("Invalid integer")
            }
            FLOAT -> try {
                Result.success(input.toFloat())
            } catch (e: Exception) {
                Result.success("Invalid float")
            }
            BOOL -> try {
                Result.success(input.toBoolean())
            } catch (e: Exception) {
                Result.success("Invalid boolean")
            }
            TUPLE -> Result.success(input.split(", ").map { it })
        }
    }

    fun validate(value: String): Boolean {
        return try {
            when (this) {
                STRING -> value
                INT -> value.toInt()
                FLOAT -> value.toFloat()
                BOOL -> value.toBoolean()
                TUPLE -> value.split(", ").map { it }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun parse(value: String): Any {
        return when (this) {
            STRING -> value
            INT -> value.toInt()
            FLOAT -> value.toFloat()
            BOOL -> value.toBoolean()
            TUPLE -> value.split(", ").map { it }
        }
    }
}