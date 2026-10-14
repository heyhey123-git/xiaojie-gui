package io.github.heyhey123.xiaojiegui.skript.elements.session.effects

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Clear Session")
@Description(
    "Clears the menu session of the given player(s).",
    "This will remove the content of the gui they have open, but will not close the gui itself.",
    "If 'refresh' is specified, the player's gui will be refreshed to reflect the cleared session.",
    "A menu, session or window keyword is required; bare 'clear' keeps Skript's variable deletion behavior."
)
@Examples(
    "clear the menu session player's menu session and refresh"
)
@Since("1.0.0")
class EffClearSession : Effect() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.effect(
                addon,
                EffClearSession::class.java,
                // Untyped variables also match menusession: never register bare `clear %menusession%`.
                "clear [the] (menu [(session|window)]|session|window) %menusession% [refresh:and refresh]",
                "clear [the] menu (session|window) of %player% [refresh:and refresh]"
            )
        }
    }

    private var exprSession: Expression<MenuSession>? = null

    private var exprPlayer: Expression<Player>? = null

    private var refreshFlag: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        if (matchedPattern == 0) {
            exprSession = expressions!![0] as Expression<MenuSession>
        } else {
            exprPlayer = expressions!![0] as Expression<Player>
        }
        refreshFlag = parseResult?.hasTag("refresh") ?: false
        return true
    }

    override fun execute(event: Event?) {
        val session = exprSession?.getSingle(event)
            ?: exprPlayer?.getSingle(event)?.let(MenuSession::querySession)
        if (session == null) {
            this.error(
                "Menu session cannot be null: ${this.toString(event, true)}"
            )
            return
        }

        session.clear(refreshFlag)
    }

    override fun toString(event: Event?, debug: Boolean) =
        "clear menu session ${exprSession?.toString(event, debug) ?: "of ${exprPlayer?.toString(event, debug)}"}" +
            if (refreshFlag) " and refresh" else ""
}
