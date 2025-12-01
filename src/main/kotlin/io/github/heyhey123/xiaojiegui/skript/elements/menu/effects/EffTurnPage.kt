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
import io.github.heyhey123.xiaojiegui.skript.utils.ComponentHelper
import io.github.heyhey123.xiaojiegui.skript.utils.TitleType
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event


@Name("Turn Page")
@Description(
    "Turn the page of the currently open menu for a player.",
    "You can optionally specify a new title for the menu."
)
@Examples(
    "turn to page 2 for player with new title \"New Page Title\""
)
@Since("1.0-SNAPSHOT")
class EffTurnPage : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffTurnPage::class.java,
                "turn to page %number% for %player% [with [new] title (string:%-string%|component:%-textcomponent%)]"
            )
        }
    }

    private lateinit var pageExpr: Expression<Number>

    private lateinit var playerExpr: Expression<Player>

    private var newTitleStrExpr: Expression<String>? = null

    private var newTitleComponentExpr: Expression<Any>? = null

    private var titleType: TitleType? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        pageExpr = expressions?.get(0) as Expression<Number>
        playerExpr = expressions[1] as Expression<Player>
        if (parseResult!!.tags.isNotEmpty()) {
            titleType = TitleType.fromParseResult(parseResult)
            newTitleStrExpr = expressions[2] as Expression<String>?
            newTitleComponentExpr = expressions[3] as Expression<Any>?
        }

        return true
    }

    override fun execute(event: Event?) {
        val page = pageExpr.getSingle(event)?.toInt()
        if (page == null) {
            Skript.error("Page number cannot be null.")
            return
        }

        val player = playerExpr.getSingle(event)
        if (player == null) {
            Skript.error("Player cannot be null.")
            return
        }

        val session = MenuSession.querySession(player)
        val menu = session?.menu
        if (session == null || menu == null) {
            Skript.error("Player $player does not have an open menu session.")
            return
        }
        if (page !in 1..<menu.size) {
            Skript.error("Page number $page is out of bounds for the menu.")
            return
        }

        val title = titleType?.let {
            ComponentHelper.resolveTitleComponentOrNull(
                newTitleStrExpr,
                newTitleComponentExpr,
                event,
                it
            )
        }

        if (enableAsyncCheck && !Bukkit.isPrimaryThread()) {
            Skript.error(
                "Menu page can only be turned from the main server thread, " +
                        "but got called from an asynchronous thread: ${Thread.currentThread().name}\n" +
                        "current statement: ${this.toString(event, true)}"
            )
            return
        }

        menu.turnPage(player, page, title)
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val sb = StringBuilder("turn to page ").append(pageExpr.toString(event, debug))
            .append(" for ").append(playerExpr.toString(event, debug))
        titleType?.let {
            sb.append(" with new title ")
            newTitleStrExpr?.let { strExpr ->
                sb.append("string:").append(strExpr.toString(event, debug))
            }
            newTitleComponentExpr?.let { compExpr ->
                sb.append("component:").append(compExpr.toString(event, debug))
            }
        }
        return sb.toString()
    }
}
