package io.github.heyhey123.xiaojiegui.logging

import io.github.heyhey123.xiaojiegui.XiaojieGUI
import kotlin.math.roundToInt

/**
 * Prints the banner the plugin greets a server with.
 *
 * The colours are written into the text rather than offered to Paper. Paper decides for itself whether a
 * console can show colour and records that decision in the `net.kyori.ansi.colorLevel` system property,
 * so on a platform it reads wrong the banner arrives grey while the terminal could have shown it in full
 * colour. The banner is the one thing here that exists to be looked at, so it carries its own colours and
 * asks nobody. Paper's file appender drops them again on the way into `logs/latest.log`, which stays
 * readable as plain text.
 *
 * Writing the escapes by hand is also what keeps this file off Adventure's internals: the serializer that
 * would do it is not public API, and reaching into it can fail while the plugin is enabling -- which is a
 * bad way for a banner to behave. `force-truecolor: false` turns the colour off.
 */
internal object LogoPrinter {

    /**
     * Where the gradient runs, from the left of the wordmark to its right. Three stops rather than two,
     * because a straight interpolation between the outer two passes through colours a fire does not have.
     */
    private val gradient = intArrayOf(0xD94818, 0xD92E18, 0xD8FA19)

    private const val ESCAPE = "\u001B["

    private const val RESET = "\u001B[0m"

    private const val DARK_GRAY = 0x555555

    private const val GRAY = 0xAAAAAA

    private const val NAME = "xiaojie-gui"

    /**
     * The wordmark, in the figlet face this author's plugins share.
     *
     * The raw string opens with a newline and the source indents every line, so both are taken off: the
     * empty check drops the former, and `trimIndent` the latter. Trailing spaces are dropped as well and
     * put back by `padEnd`, which is what gives one gradient across the whole wordmark instead of one per
     * line.
     */
    private val wordmark: List<String> = """
         __   ___             _ _       _____ _    _ _____
         \ \ / (_)           (_|_)     / ____| |  | |_   _|
          \ V / _  __ _  ___  _ _  ___| |  __| |  | | | |
           > < | |/ _` |/ _ \| | |/ _ \ | |_ | |  | | | |
          / . \| | (_| | (_) | | |  __/ |__| | |__| |_| |_
         /_/ \_\_|\__,_|\___/| |_|\___|\_____|\____/|_____|
                            _/ |
                           |__/
    """.trimIndent().lines().map(String::trimEnd).filter(String::isNotEmpty)

    /** Prints the banner, with [version] centred on the line under the wordmark. */
    fun print(version: String) {
        val width = wordmark.maxOf(String::length)
        val borderWidth = width + 4
        val border = "-".repeat(borderWidth)
        val credit = "$NAME $version"
        val margin = " ".repeat(((borderWidth - credit.length) / 2).coerceAtLeast(0))

        send(listOf(Span(border, DARK_GRAY)))
        wordmark.forEach { line -> send(gradientSpans(line.padEnd(width))) }
        send(listOf(Span("$margin$NAME ", GRAY)) + gradientSpans(version))
        send(listOf(Span(border, DARK_GRAY)))
    }

    /** A run of text in one colour, so a line can be written as escape sequences without losing it. */
    private data class Span(val text: String, val colour: Int)

    /**
     * Sends one line to the console, colours and all.
     *
     * The logger is what gets the escape sequences past Paper's own rendering, and past its file
     * appender, which drops them again for the log.
     */
    private fun send(spans: List<Span>) {
        XiaojieGUI.instance.logger.info(ansi(spans))
    }

    /**
     * The spans as true-colour escape sequences, or as plain text when `force-truecolor` is off.
     *
     * `38;2;r;g;b` picks a foreground colour, and the reset at the end keeps it out of everything that
     * follows the line. Doing this by hand is what makes the colours survive a console Paper has decided
     * cannot show them.
     */
    private fun ansi(spans: List<Span>): String {
        if (!XiaojieGUI.forceTrueColor) return spans.joinToString("") { it.text }

        return buildString {
            spans.forEach { span ->
                append(ESCAPE)
                    .append("38;2;")
                    .append(span.colour shr 16 and 0xFF).append(';')
                    .append(span.colour shr 8 and 0xFF).append(';')
                    .append(span.colour and 0xFF).append('m')
                    .append(span.text)
            }
            append(RESET)
        }
    }

    /** [text] painted one character at a time, so that the gradient is not lost on any of it. */
    private fun gradientSpans(text: String): List<Span> {
        val last = (text.length - 1).coerceAtLeast(1)
        return text.mapIndexed { index, character ->
            Span(character.toString(), colourAt(index.toDouble() / last))
        }
    }

    /** The colour [progress] of the way along the gradient. */
    private fun colourAt(progress: Double): Int {
        val scaled = progress.coerceIn(0.0, 1.0) * (gradient.size - 1)
        val stop = scaled.toInt().coerceAtMost(gradient.size - 2)
        val part = scaled - stop
        val from = gradient[stop]
        val to = gradient[stop + 1]
        val red = blend(from shr 16 and 0xFF, to shr 16 and 0xFF, part)
        val green = blend(from shr 8 and 0xFF, to shr 8 and 0xFF, part)
        val blue = blend(from and 0xFF, to and 0xFF, part)
        return red shl 16 or (green shl 8) or blue
    }

    private fun blend(from: Int, to: Int, part: Double): Int = (from + (to - from) * part).roundToInt()
}
