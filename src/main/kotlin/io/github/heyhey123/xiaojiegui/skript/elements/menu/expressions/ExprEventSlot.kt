package io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import org.bukkit.event.Event

class ExprEventSlot : SimpleExpression<Number>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprEventSlot::class.java,
                Number::class.java,
                ExpressionType.SIMPLE,
                "[the] [event-]clicked slot" // event-slot causes conflict with skript
            )
        }
    }

    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean =
        parser.isCurrentEvent(MenuInteractEvent::class.java)

    override fun get(event: Event?): Array<Number> =
        if (event !is MenuInteractEvent) arrayOf()
        else arrayOf(event.slot)

    override fun toString(event: Event?, debug: Boolean) =
        "the event-clicked slot"

    override fun isSingle() = true

    override fun getReturnType() = Number::class.java
}
