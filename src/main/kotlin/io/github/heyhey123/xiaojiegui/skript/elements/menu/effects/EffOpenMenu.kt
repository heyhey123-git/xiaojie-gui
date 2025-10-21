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
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
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
                "(open|show) [the] (menu|gui) %menu% (for|to) %player% [and (turn to|go to|on) page %-number%]"
            )
        }
    }

    private lateinit var menu: Expression<Menu>

    private lateinit var player: Expression<Player>

    private var page: Expression<Number>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        menu = expressions?.get(0) as Expression<Menu>
        player = expressions[1] as Expression<Player>
        page = expressions[2] as Expression<Number>?
        return true
    }

    override fun execute(event: Event?) {
        val menu = menu.getSingle(event) ?: return
        val player = player.getSingle(event) ?: return
        val pageNum = page?.getSingle(event)?.toInt()

        check(!enableAsyncCheck || Bukkit.isPrimaryThread()) {
            "Menu can only be opened from the main server thread, " +
                    "but got called from an asynchronous thread: ${Thread.currentThread().name}\n" +
                    "current statement: ${this.toString(event, true)}"
        }

        if (pageNum != null) {
            menu.open(player, pageNum)
            return
        }
        menu.open(player)
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val str = "open menu ${menu.toString(event, debug)} for ${player.toString(event, debug)}"
        return if (page != null) "$str and go to page ${page.toString()}" else str
    }

}
