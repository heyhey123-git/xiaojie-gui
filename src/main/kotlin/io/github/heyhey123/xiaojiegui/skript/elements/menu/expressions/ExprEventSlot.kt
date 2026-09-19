package io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Clicked Slot")
@Description("The slot the player clicked, in `on menu interact`.")
@Examples(
    "on menu interact:",
    "    send \"You clicked slot %the clicked slot%.\" to player"
)
@Since("1.0.0")
class ExprEventSlot : SimpleExpression<Number>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.expression(
                addon,
                ExprEventSlot::class.java,
                Number::class.java,
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
        if (event !is MenuInteractEvent) {
            emptyArray()
        } else {
            arrayOf(event.slot)
        }

    override fun toString(event: Event?, debug: Boolean) =
        "the event-clicked slot"

    override fun isSingle() = true

    override fun getReturnType() = Number::class.java
}
