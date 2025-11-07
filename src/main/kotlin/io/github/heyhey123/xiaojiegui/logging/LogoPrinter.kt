package io.github.heyhey123.xiaojiegui.logging

import io.github.heyhey123.xiaojiegui.XiaojieGUI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.flattener.ComponentFlattener
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer
import net.kyori.ansi.ColorLevel
import org.bukkit.Bukkit

internal object LogoPrinter {

    /**
     * Hex colors used in the logo gradient. (fire theme)
     */
    private val hex = arrayOf(
        "#D94818",
        "#D92E18",
        "#D8FA19"
    )

    /**
     * ASCII art logo.
     */
    private const val LOGO: String =
        """
 __   ___             _ _       _____ _    _ _____ 
 \ \ / (_)           (_|_)     / ____| |  | |_   _|
  \ V / _  __ _  ___  _ _  ___| |  __| |  | | | |  
   > < | |/ _` |/ _ \| | |/ _ \ | |_ | |  | | | |  
  / . \| | (_| | (_) | | |  __/ |__| | |__| |_| |_ 
 /_/ \_\_|\__,_|\___/| |_|\___|\_____|\____/|_____|
                    _/ |                           
                   |__/                             
        """

    /**
     * Border used in the logo printout.
     */
    private const val BORDER_LINE = "-------------------------------------------------------"

    /**
     * System property key for overriding the ANSI color level.
     */
    private const val COLOR_LEVEL_PROPERTY = "net.kyori.ansi.colorLevel"

    /**
     * ANSI serializer instance that always uses "truecolor" color level when serializing,
     * unless overridden by system property.
     */
    private val serializer: ANSIComponentSerializer by lazy {
        val ansiComponentSerializer = Class
            .forName("net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializerImpl")
            .getDeclaredConstructor(ColorLevel::class.java, ComponentFlattener::class.java)
            .apply { isAccessible = true }
            .newInstance(ColorLevel.TRUE_COLOR, ComponentFlattener.basic()) as ANSIComponentSerializer

        return@lazy ansiComponentSerializer
    }

    /**
     * Serialize a component to ANSI string.
     *
     * @param component the component to serialize
     * @return the ANSI string
     */
    private fun serializeToAnsi(component: Component): String {
        return serializer.serialize(component)
    }

    /**
     * Generate logo lines with gradient applied.
     *
     * @param hex the hex colors for the gradient
     * @return the list of components representing the logo lines
     */
    private fun logoLinesGradient(vararg hex: String): List<Component> {
        var open = "<gradient:"
        for ((index, h) in hex.withIndex()) {
            open += h
            if (index != hex.size - 1) open += ":"
            if (index == hex.size - 1) open += "> "
        }

        val close = " </gradient>"
        return LOGO.trimStart('\n').lines().map { line ->
            if (line.isBlank()) Component.empty()
            else MiniMessage.miniMessage().deserialize("$open$line$close")
        }
    }

    /**
     * Print the logo to the console with the specified version.
     *
     * @param version the version string to display
     */
    fun print(version: String) {
        val console = Bukkit.getConsoleSender()
        val border = Component.text(BORDER_LINE, NamedTextColor.DARK_GRAY)

        if (!XiaojieGUI.forceTrueColor || System.getProperty(COLOR_LEVEL_PROPERTY) != null) {
            console.sendMessage(border)
            logoLinesGradient(*hex).forEach(console::sendMessage)
            console.sendMessage(MiniMessage.miniMessage().deserialize("<gray>v</gray><rainbow>$version</rainbow>"))
            console.sendMessage(border)
            return
        }

        val logger = XiaojieGUI.instance.logger
        val serializedBorder = serializeToAnsi(border)

        logger.info(serializedBorder)
        for (line in logoLinesGradient(*hex)) {
            logger.info(serializeToAnsi(line))
        }
        logger.info(serializeToAnsi(
            MiniMessage.miniMessage().deserialize("<gray>v</gray><rainbow>$version</rainbow>")
        ))
        logger.info(serializedBorder)
    }
}
