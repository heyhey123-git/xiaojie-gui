package io.github.heyhey123.xiaojiegui.skript.elements.menu.effects

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
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Turn Page")
@Description(
    "Turn the page of the currently open menu for a player.",
    "You can optionally specify a new title for the menu: quoted text, or a value that already is a text component."
)
@Examples(
    "turn to page 2 for player with new title \"New Page Title\""
)
@Since("1.0.0")
class EffTurnPage : Effect() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.effect(
                addon,
                EffTurnPage::class.java,
                // Three patterns, one per title form plus one without a title, instead of a single
                // optional group with two alternatives: when the alternatives of a group have different
                // types, Skript hands the literal over unparsed (see EffSecCreateMenu), and the group's
                // two slots pushed the title away from a fixed index. A pattern of its own gives the
                // title slot a single type, always at index 2; the titleless pattern leaves it null.
                "turn to page %number% for %player%",
                "turn to page %number% for %player% [with [new] title string:%-string%]",
                "turn to page %number% for %player% [with [new] title %-object%]"
            )
        }
    }

    private lateinit var pageExpr: Expression<Number>

    private lateinit var playerExpr: Expression<Player>

    private var titleExpr: Expression<Any>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        // Skript always passes the array; naming it once makes the three reads below plain indexing
        // instead of a chain of safe calls that say nothing about whether a later one can be null.
        val exprs = expressions ?: return false
        pageExpr = exprs[0] as Expression<Number>
        playerExpr = exprs[1] as Expression<Player>
        // Null in the titleless pattern; the other two keep the title at the same index.
        titleExpr = exprs.getOrNull(2) as Expression<Any>?

        return true
    }

    override fun execute(event: Event?) {
        val page = pageExpr.getSingle(event)?.toInt()
        if (page == null) {
            error("Page number cannot be null.")
            return
        }

        val player = playerExpr.getSingle(event)
        if (player == null) {
            error("Player cannot be null.")
            return
        }

        val session = MenuSession.querySession(player)
        val menu = session?.menu
        if (session == null || menu == null) {
            error("Player $player does not have an open menu session.")
            return
        }

        val title = ComponentHelper.resolveTitleComponentOrNull(titleExpr, event, this)

        if (enableAsyncCheck && !Bukkit.isPrimaryThread()) {
            error(
                "Menu page can only be turned from the main server thread, " +
                    "but got called from an asynchronous thread: ${Thread.currentThread().name}\n" +
                    "current statement: ${this.toString(event, true)}"
            )
            return
        }

        if (page !in 1..menu.size) {
            error("Page $page does not exist in this menu.")
            return
        }
        if (menu.pages[page].layout != session.receptacle?.layout) {
            error("Cannot turn to page $page with a different inventory layout. The current page is unchanged.")
            return
        }
        menu.turnPage(player, page, title)
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val sb = StringBuilder("turn to page ").append(pageExpr.toString(event, debug))
            .append(" for ").append(playerExpr.toString(event, debug))
        titleExpr?.let {
            sb.append(" with new title ").append(it.toString(event, debug))
        }
        return sb.toString()
    }
}
