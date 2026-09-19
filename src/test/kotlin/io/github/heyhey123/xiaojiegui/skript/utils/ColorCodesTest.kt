package io.github.heyhey123.xiaojiegui.skript.utils

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The `&` to `§` translation, checked without a server: it is a pure text function, and it is what makes
 * `&a` in a menu title mean colour rather than three literal characters.
 */
class ColorCodesTest {

    @Test
    fun `ampersand codes become section codes`() {
        assertEquals("\u00A7aGreen \u00A7lBold", translateAmpersandColorCodes("&aGreen &lBold"))
    }

    @Test
    fun `section codes are left as they are`() {
        assertEquals("\u00A7aGreen", translateAmpersandColorCodes("\u00A7aGreen"))
    }

    @Test
    fun `the code character is lowercased like Spigot does`() {
        assertEquals("\u00A7aGreen", translateAmpersandColorCodes("&AGreen"))
    }

    @Test
    fun `an ampersand that is not a code stays`() {
        assertEquals("Tom & Jerry", translateAmpersandColorCodes("Tom & Jerry"))
    }

    @Test
    fun `a hex sequence is translated code by code`() {
        // Every digit of Adventure's hex form is itself code-prefixed, so each one is translated.
        assertEquals(
            "\u00A7x\u00A7f\u00A7f\u00A70\u00A70\u00A70\u00A70Text",
            translateAmpersandColorCodes("&x&f&f&0&0&0&0Text")
        )
    }

    @Test
    fun `a letter that is not a hex digit is not a code in the middle of one`() {
        // `g` is not a colour, a format or a hex digit, so `&g` is left as written.
        assertEquals("\u00A7x\u00A7f\u00A7f&g", translateAmpersandColorCodes("&x&f&f&g"))
    }
}
