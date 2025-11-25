package io.github.heyhey123.xiaojiegui.skript.utils

import ch.njol.skript.lang.SkriptParser

/**
 * The type of title used in menu creation.
 */
enum class TitleType {
    STRING,
    COMPONENT;

    companion object {
        /**
         * Converts a string representation to the corresponding TitleType.
         *
         * @param parseResult the SkriptParser.ParseResult containing tags
         * @return the corresponding TitleType
         * @throws IllegalArgumentException if the string does not match any TitleType
         */
        fun fromParseResult(parseResult: SkriptParser.ParseResult): TitleType =
            when  {
                parseResult.tags.contains("string") -> STRING
                parseResult.tags.contains("component") -> COMPONENT
                else -> throw IllegalArgumentException("Unknown TitleType in ParseResult")
            }
    }
}
