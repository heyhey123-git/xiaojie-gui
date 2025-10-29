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
import io.github.heyhey123.xiaojiegui.XiaojieGUI.Companion.enableAsyncCheck
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import org.bukkit.Bukkit
import org.bukkit.event.Event


@Name("Refresh Menu Session")
@Description(
    "Refresh the menu session, updating the inventory view.",
    "If a slot number is provided, only that slot will be refreshed.",
    "If no slot number is provided, the entire inventory will be refreshed.",
    "Does nothing if the menu receptacle mode is static or the session is not active."
)
@Examples(
    "refresh the slot 5 in menu session {_session}",
    "refresh menu session {_session}"
)
@Since("1.0-SNAPSHOT")
class EffRefreshSession : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffRefreshSession::class.java,
                "refresh [the slot %-number% in] [the] [menu] [session] %menusession%"
            )
        }
    }

    private var slotExpr: Expression<Number>? = null

    private lateinit var sessionExpr: Expression<MenuSession>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        slotExpr = expressions?.get(0) as Expression<Number>?
        sessionExpr = expressions?.get(1) as Expression<MenuSession>
        return true
    }

    override fun execute(event: Event?) {
        val session = sessionExpr.getSingle(event) ?: return
        if (session.receptacle?.mode != Receptacle.Mode.PHANTOM) return

        val slot = slotExpr?.getSingle(event)?.toInt() ?: -1

        if (enableAsyncCheck && !Bukkit.isPrimaryThread()) {
            Skript.error(
                "Menu session can only be refresh from the main server thread," +
                        "but got called from an asynchronous thread: ${Thread.currentThread().name}\n" +
                        "current statement: ${this.toString(event, true)}"
            )
            return
        }

        session.refresh(slot)
    }

    override fun toString(event: Event?, debug: Boolean) =
        "refresh ${if (slotExpr != null) "the slot ${slotExpr!!.toString(event, debug)} in " else ""}${
            sessionExpr.toString(
                event,
                debug
            )
        }"

}
