package io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.event.MenuOpenEvent
import io.github.heyhey123.xiaojiegui.gui.event.PageTurnEvent
import org.bukkit.event.Event

class ExprEventPage : SimpleExpression<Number>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprEventPage::class.java,
                Number::class.java,
                ExpressionType.SIMPLE,
                "[the] [event-]page",
                "[the] future [event-]page"
            )
        }
    }

    private var isFuture: Boolean = false

    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        isFuture = matchedPattern == 1
        return parser.currentEvents?.any { c ->
            c == MenuOpenEvent::class.java ||
                    c == MenuInteractEvent::class.java ||
                    c == PageTurnEvent::class.java
        } ?: false
    }


    override fun get(event: Event?): Array<Number> =
        when (event) {
            is MenuOpenEvent -> {
                arrayOf(event.page)
            }

            is MenuInteractEvent -> {
                arrayOf(event.page)
            }

            is PageTurnEvent -> {
                arrayOf(if (isFuture) event.from else event.to)
            }

            else -> arrayOf()
        }

    override fun toString(event: Event?, debug: Boolean) =
        if (isFuture) "the future event-page" else "the event-page"

    override fun isSingle() = true

    override fun getReturnType() = Number::class.java
}
