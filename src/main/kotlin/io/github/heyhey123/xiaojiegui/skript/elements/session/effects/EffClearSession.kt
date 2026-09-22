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
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Clear Session")
@Description(
    "Clears the menu session of the given player(s).",
    "This will remove the content of the gui they have open, but will not close the gui itself.",
    "If 'refresh' is specified, the player's gui will be refreshed to reflect the cleared session."
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
                "clear [the] [menu] [(session|window)] %menusession% [refresh:and refresh]"
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
            this.error(
                "Menu session cannot be null: ${this.toString(event, true)}"
            )
            return
        }

        session.clear(refreshFlag)
    }

    override fun toString(event: Event?, debug: Boolean) =
        "clear menu session ${exprSession.toString(event, debug)}${if (refreshFlag) " and refresh" else ""}"
}
