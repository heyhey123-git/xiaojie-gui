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
import io.github.heyhey123.xiaojiegui.skript.utils.ComponentHelper
import io.github.heyhey123.xiaojiegui.skript.utils.TitleType
import org.bukkit.Bukkit
import org.bukkit.event.Event


@Name("Update Menu Session Title")
@Description(
    "Update the title of a menu session.",
    "If 'and refresh' is included, the inventory view will be refreshed to reflect the new title immediately."
)
@Examples(
    "update title of menu session {_session} to string:\"New Title\" and refresh"
)
@Since("1.0-SNAPSHOT")
class EffUpdateSessionTitle : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffUpdateSessionTitle::class.java,
                "update title of [the] [menu] [session] %menusession% to (string:%-string%|component:%-textcomponent%) [refresh:(and refresh)]"
            )
        }
    }

    private lateinit var sessionExpr: Expression<MenuSession>

    private var titleStrExpr: Expression<String>? = null

    private var titleComponentExpr: Expression<Any>? = null

    private lateinit var titleTypeExpr: TitleType

    private var refreshFlag: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        sessionExpr = expressions?.get(0) as Expression<MenuSession>
        titleStrExpr = expressions[1] as Expression<String>?
        titleComponentExpr = expressions[2] as Expression<Any>?
        titleTypeExpr = TitleType.fromParseResult(parseResult!!)
        refreshFlag = parseResult.hasTag("refresh")

        return true
    }

    override fun execute(event: Event?) {
        val session = sessionExpr.getSingle(event)
        if (session == null) {
            Skript.error("Menu session cannot be null when updating title.")
            return
        }
        val title = ComponentHelper.resolveTitleComponentOrNull(
            titleStrExpr,
            titleComponentExpr,
            event,
            titleTypeExpr
        )
        if (title == null) {
            Skript.error("Valid title is required.")
            return
        }
        if (enableAsyncCheck && !Bukkit.isPrimaryThread()) {
            Skript.error(
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
            (titleStrExpr ?: titleComponentExpr)?.toString(
                event,
                debug
            )
        }${if (refreshFlag) " and refresh" else ""}"
}
