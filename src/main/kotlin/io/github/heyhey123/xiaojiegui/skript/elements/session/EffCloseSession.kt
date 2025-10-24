package io.github.heyhey123.xiaojiegui.skript.elements.session

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
import org.bukkit.event.Event


@Name("Close Menu Session")
@Description(
    "Closes a menu session, effectively closing the menu for the player."
)
@Examples(
    "close the menu session of player"
)
@Since("1.0-SNAPSHOT")
class EffCloseSession : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffCloseSession::class.java,
                "close [the] %menusession%"
            )
        }
    }

    private lateinit var session: Expression<MenuSession>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        session = expressions?.get(0) as Expression<MenuSession>
        return true
    }

    override fun execute(event: Event?) {
        val session = session.getSingle(event)
        if (session == null) {
            Skript.error(
                "Menu session cannot be null: ${this.toString(event, true)}"
            )
            return
        }

        if (enableAsyncCheck && !Bukkit.isPrimaryThread()) {
            Skript.error(
                "Menu session can only be closed from the main server thread, " +
                        "but got called from an asynchronous thread: ${Thread.currentThread().name}\n" +
                        "current statement: ${this.toString(event, true)}"
            )
        }

        session.close()
    }

    override fun toString(event: Event?, debug: Boolean) =
        "close ${session.toString(event, debug)}"
}
