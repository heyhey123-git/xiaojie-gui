package io.github.heyhey123.xiaojiegui.skript.elements.menu.effects

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.XiaojieGUI.Companion.enableAsyncCheck
import io.github.heyhey123.xiaojiegui.gui.event.MenuEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event


@Name("Open Menu")
@Description(
    "Open a menu for a player.",
    "You can optionally specify a page number to open a specific page of the menu."
)
@Examples(
    "open menu {_menu} for player",
    "open menu {_menu} for player and go to page 2"
)
@Since("1.0-SNAPSHOT")
class EffOpenMenu : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffOpenMenu::class.java,
                "(open|show) [the] (menu|gui) [%-menu%] (for|to) %player% [and (turn to|go to|on) page %-number%]"
            )
        }
    }

    private var menuExpr: Expression<Menu>? = null

    private lateinit var playerExpr: Expression<Player>

    private var pageExpr: Expression<Number>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        menuExpr = expressions?.get(0) as Expression<Menu>?
        if (menuExpr == null && !parser.isCurrentEvent(MenuEvent::class.java, ProvideMenuEvent::class.java)) {
            Skript.error("Menu expression is required if the current event is not a menu-related event.")
            return false
        }
        playerExpr = expressions!![1] as Expression<Player>
        pageExpr = expressions[2] as Expression<Number>?
        return true
    }

    override fun execute(event: Event?) {
        val menu = menuExpr?.getSingle(event) ?: when (event) {
            is MenuEvent -> event.menu
            is ProvideMenuEvent -> event.menu
            else -> null
        }

        if (menu == null) {
            Skript.error("Failed to get the menu to open. Please check your code.")
            return
        }
        val player = playerExpr.getSingle(event)
        if (player == null) {
            Skript.error(
                "Player expression returned null. Cannot open menu."
            )
            return
        }

        val pageNum = pageExpr?.getSingle(event)?.toInt()

        if (enableAsyncCheck && !Bukkit.isPrimaryThread()) {
            Skript.error(
                "Menu can only be opened from the main server thread, " +
                        "but got called from an asynchronous thread: ${Thread.currentThread().name}\n" +
                        "current statement: ${this.toString(event, true)}"
            )
        }

        if (pageNum != null) {
            menu.open(player, pageNum)
            return
        }
        menu.open(player)
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val sb = StringBuilder("open menu ")
        sb.append(menuExpr?.toString(event, debug) ?: "event menu")
        sb.append(" for ").append(playerExpr.toString(event, debug))
        pageExpr?.let {
            sb.append(" and go to page ").append(it.toString(event, debug))
        }
        return sb.toString()
    }
}
