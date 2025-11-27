package io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.bukkit.entity.Player
import org.bukkit.event.Event

@Name("Menu Current Page")
@Description(
    "Get the current page index of a player's menu session.",
    "This expression returns the current page index of the specified player's menu session."
)
@Examples(
    "set {_page} to player's current menu page",
    "send \"You are currently on page %{_page}% of the menu.\" to player"
)
@Since("1.0.4")
class ExprCurrentPage: SimpleExpression<Number>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprCurrentPage::class.java,
                Number::class.java,
                ExpressionType.SIMPLE,
                "%player%'s current [(menu|gui)] page",
                "[the] current [(menu|gui)] page of %player%"
            )
        }
    }

    private lateinit var playerExpr: Expression<Player>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        playerExpr = expressions!![0] as Expression<Player>
        return true
    }

    override fun get(event: Event?): Array<Number> {
        val player = playerExpr.getSingle(event) ?: return emptyArray()
        val session = MenuSession.querySession(player)
            ?: return emptyArray()
        return arrayOf(session.page)
    }

    override fun toString(event: Event?, debug: Boolean) =
        "${playerExpr.toString(event, debug)}'s current menu page"

    override fun isSingle() = true

    override fun getReturnType() = Number::class.java

}
