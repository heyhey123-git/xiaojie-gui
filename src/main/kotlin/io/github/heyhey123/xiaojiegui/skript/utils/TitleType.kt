package io.github.heyhey123.xiaojiegui.skript.utils

/**
 * The type of title used in menu creation.
 */
enum class TitleType {
    STRING,
    COMPONENT;

    companion object {
        /**
         * Get the TitleType from a string representation.
         *
         * @param str the string representation
         * @return the corresponding TitleType
         * @throws IllegalArgumentException if the string does not match any TitleType
         */
        fun fromStringTag(str: String): TitleType =
            when (str) {
                "string" -> STRING
                "component" -> COMPONENT
                else -> throw IllegalArgumentException("Unknown TitleType: $str")
            }
    }
}
