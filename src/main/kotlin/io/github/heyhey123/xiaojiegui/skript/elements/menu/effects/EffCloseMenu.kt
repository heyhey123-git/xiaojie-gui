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
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event

@Name("Close Menu")
@Description(
    "Close the menu for a player (if opened)."
)
@Examples(
    "close the menu for player"
)
@Since("1.0-SNAPSHOT")
class EffCloseMenu : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffCloseMenu::class.java,
                "(close|close down|shut) [the] (menu|gui) (for|to) %player%"
            )
        }
    }

    private lateinit var playerExpr: Expression<Player>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        playerExpr = expressions?.get(0) as Expression<Player>
        return true
    }

    override fun execute(event: Event?) {
        val player = playerExpr.getSingle(event) ?: return

        if (enableAsyncCheck && !Bukkit.isPrimaryThread()) {
            Skript.error(
                "Menu can only be closed from the main server thread, " +
                        "but got called from an asynchronous thread: ${Thread.currentThread().name}\n" +
                        "current statement: ${this.toString(event, true)}"
            )
        }

        MenuSession.querySession(player)?.close() ?: Skript.error(
            "Cannot close menu for player ${player.name} because they do not have an open menu."
        )
    }

    override fun toString(event: Event?, debug: Boolean): String =
        "close the menu for ${playerExpr.toString(event, debug)}"

}
