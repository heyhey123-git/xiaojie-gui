package io.github.heyhey123.xiaojiegui.skript.elements.session.effects

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.bukkit.event.Event

@Name("Clear Session")
@Description(
    "Clears the menu session of the given player(s).",
    "This will remove the content of the gui they have open, but will not close the gui itself.",
    "If 'refresh' is specified, the player's gui will be refreshed to reflect the cleared session."
)
@Examples(
    "if the menu with id \"main_menu\" is destroyed:",
    "    send \"The main menu has been destroyed and can no longer be used.\" to player"
)
@Since("1.0-SNAPSHOT")
class EffClearSession: Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffClearSession::class.java,
                "clear [the] [menu] [session] %menusession% [refresh:and refresh]"
            )
        }
    }

    private lateinit var exprSession: Expression<MenuSession>

    private var refreshFlag: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        exprSession = expressions!![0] as Expression<MenuSession>
        refreshFlag = parseResult?.hasTag("refresh") ?: false
        return true
    }

    override fun execute(event: Event?) {
        val session = exprSession.getSingle(event)
        if (session == null) {
            Skript.error(
                "Menu session cannot be null: ${this.toString(event, true)}"
            )
            return
        }

        session.clear(refreshFlag)
    }

    override fun toString(event: Event?, debug: Boolean) =
        "clear menu session ${exprSession.toString(event, debug)}${if (refreshFlag) " and refresh" else ""}"
}
