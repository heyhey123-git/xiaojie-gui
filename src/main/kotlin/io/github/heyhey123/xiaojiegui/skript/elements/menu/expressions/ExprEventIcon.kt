package io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

class ExprEventIcon : SimpleExpression<ItemStack>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprEventIcon::class.java,
                ItemStack::class.java,
                ExpressionType.SIMPLE,
                "[the] [event-]icon"
            )
        }
    }

    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ) =
        parser.isCurrentEvent(MenuInteractEvent::class.java)


    override fun get(event: Event?): Array<ItemStack?> {
        val e = event as? MenuInteractEvent ?: return arrayOf()
        return arrayOf(e.icon)
    }

    override fun toString(event: Event?, debug: Boolean) = "the event-icon"

    override fun isSingle() = true

    override fun getReturnType() = ItemStack::class.java
}
