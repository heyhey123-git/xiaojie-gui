package io.github.heyhey123.xiaojiegui.skript.utils

/**
 * Turn `&` colour codes into the `§` form Adventure's legacy serializer reads.
 *
 * This is the same translation Spigot applies to item names: `&` followed by a code character becomes
 * `§`, and anything else is left alone. Both forms therefore work in a title, a title that really
 * contains `&` still shows it, and hex colours work in Adventure's `&x&r&r&g&g&b&b` form because every
 * `&` in it is translated like any other code.
 *
 * The accepted characters are Spigot's set: `0`-`9` and `a`-`f` are colours, `k`-`o` are formats, `r`
 * resets, and `x` starts a hex sequence.
 */
internal fun translateAmpersandColorCodes(text: String): String {
    if ('&' !in text) return text

    val builder = StringBuilder(text.length)
    var index = 0
    while (index < text.length) {
        val current = text[index]
        val next = text.getOrNull(index + 1)
        if (current == '&' && next != null && next in LEGACY_CODE_CHARACTERS) {
            builder.append('\u00A7').append(next.lowercaseChar())
            index += 2
        } else {
            builder.append(current)
            index++
        }
    }
    return builder.toString()
}

private const val LEGACY_CODE_CHARACTERS = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx"
