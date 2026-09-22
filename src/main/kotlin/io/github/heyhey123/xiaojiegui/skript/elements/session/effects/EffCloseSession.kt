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
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Close Menu Session")
@Description(
    "Closes a menu session, effectively closing the menu for the player."
)
@Examples(
    "close the menu session of player"
)
@Since("1.0.0")
class EffCloseSession : Effect() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.effect(
                addon,
                EffCloseSession::class.java,
                "close [the] [menu] [(session|window)] %menusession%"
            )
        }
    }

    private lateinit var sessionExpr: Expression<MenuSession>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        sessionExpr = expressions?.get(0) as Expression<MenuSession>
        return true
    }

    override fun execute(event: Event?) {
        val session = sessionExpr.getSingle(event)
        if (session == null) {
            this.error(
                "Menu session cannot be null: ${this.toString(event, true)}"
            )
            return
        }

        if (!Bukkit.isPrimaryThread()) {
            this.error(
                "Menu session can only be closed from the main server thread, " +
                    "but got called from an asynchronous thread: ${Thread.currentThread().name}\n" +
                    "current statement: ${this.toString(event, true)}"
            )
            return
        }

        session.close()
    }

    override fun toString(event: Event?, debug: Boolean) =
        "close ${sessionExpr.toString(event, debug)}"
}
