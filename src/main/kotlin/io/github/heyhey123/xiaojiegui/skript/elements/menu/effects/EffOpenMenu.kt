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
import io.github.heyhey123.xiaojiegui.gui.event.MenuEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Open Menu")
@Description(
    "Open a menu for a player.",
    "You can optionally specify a page number to open a specific page of the menu."
)
@Examples(
    // Both forms are shown inside a menu event on purpose: outside one the menu expression is required,
    // so the second line cannot parse in a command trigger.
    "on menu interact:",
    "    open menu {_menu} for player and go to page 2",
    "    open menu for player # the menu of the current event, when it is left out"
)
@Since("1.0.0")
class EffOpenMenu : Effect() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.effect(
                addon,
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
            this.error("Failed to get the menu to open. Please check your code.")
            return
        }

        // A static menu shows a real inventory, and Bukkit cannot create every window: the merchant's contents
        // come from the merchant API. Saying so here is the difference between a script author reading one line
        // and reading a stack trace; it is checked before the player because no player can make it work.
        if (menu.properties.mode == Receptacle.Mode.STATIC && !menu.inventoryType.isCreatable) {
            this.error(
                "Unsupported inventory type: ${menu.inventoryType}. A static menu shows a real inventory, and " +
                    "Bukkit cannot create this one, so there is nothing to open. Use phantom mode for it."
            )
            return
        }
        val player = playerExpr.getSingle(event)
        if (player == null) {
            this.error(
                "Player expression returned null. Cannot open menu."
            )
            return
        }

        val pageNum = pageExpr?.getSingle(event)?.toInt()

        if (!Bukkit.isPrimaryThread()) {
            this.error(
                "Menu can only be opened from the main server thread, " +
                    "but got called from an asynchronous thread: ${Thread.currentThread().name}\n" +
                    "current statement: ${this.toString(event, true)}"
            )
            return
        }

        val targetPage = pageNum ?: menu.properties.defaultPage
        if (targetPage !in 1..menu.size) {
            this.error("Page $targetPage does not exist in this menu.")
            return
        }
        val session = MenuSession.querySession(player)
        if (
            session?.menu == menu &&
            session.page != targetPage &&
            menu.pages[targetPage].layout != session.receptacle?.layout
        ) {
            this.error("Cannot turn to page $targetPage with a different inventory layout. The current page is unchanged.")
            return
        }
        menu.open(player, targetPage)
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
