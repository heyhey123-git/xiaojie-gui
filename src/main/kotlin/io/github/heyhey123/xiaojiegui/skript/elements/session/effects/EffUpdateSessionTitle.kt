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
import io.github.heyhey123.xiaojiegui.skript.utils.ComponentHelper
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Update Menu Session Title")
@Description(
    "Update the title of a menu session.",
    "The title is quoted text, or a value that already is a text component.",
    "If 'and refresh' is included, the inventory view will be refreshed to reflect the new title immediately."
)
@Examples(
    "update title of menu session {_session} to \"New Title\" and refresh"
)
@Since("1.0.0")
class EffUpdateSessionTitle : Effect() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.effect(
                addon,
                EffUpdateSessionTitle::class.java,
                // One pattern per title form instead of a single group with two alternatives: when the
                // alternatives of a group have different types, Skript hands the literal over unparsed
                // (see EffSecCreateMenu) and the group's two slots moved every later slot. Both patterns
                // put the title at index 1, next to the session slot.
                "update title of [the] [menu] [(session|window)] %menusession% " +
                    "to string:%-string% [refresh:(and refresh)]",
                "update title of [the] [menu] [(session|window)] %menusession% " +
                    "to %-object% [refresh:(and refresh)]"
            )
        }
    }

    private lateinit var sessionExpr: Expression<MenuSession>

    private var titleExpr: Expression<Any>? = null

    private var refreshFlag: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        // Skript always passes the array; naming it once keeps the reads below plain indexing.
        val exprs = expressions ?: return false
        sessionExpr = exprs[0] as Expression<MenuSession>
        titleExpr = exprs.getOrNull(1) as Expression<Any>?
        // Only the refresh tag is left in this pattern; the title form is the pattern that matched.
        refreshFlag = parseResult!!.hasTag("refresh")

        return true
    }

    override fun execute(event: Event?) {
        val session = sessionExpr.getSingle(event)
        if (session == null) {
            this.error("Menu session cannot be null when updating title.")
            return
        }
        val title = ComponentHelper.resolveTitleComponentOrNull(titleExpr, event, this)
        if (title == null) {
            this.error("Valid title is required.")
            return
        }
        if (!Bukkit.isPrimaryThread()) {
            this.error(
                "Menu session title can only be updated from the main server thread, " +
                    "but got called from an asynchronous thread: ${Thread.currentThread().name}\n" +
                    "current statement: ${this.toString(event, true)}"
            )
            return
        }

        session.title(title, refreshFlag)
    }

    override fun toString(event: Event?, debug: Boolean) =
        "update title of ${sessionExpr.toString(event, debug)} to ${
            titleExpr?.toString(
                event,
                debug
            )
        }${if (refreshFlag) " and refresh" else ""}"
}
