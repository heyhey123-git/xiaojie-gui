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

@Name("Dragged Slots")
@Description(
    "The slots a drag touched, in `on menu interact`, in ascending order.",
    "It is empty for a click, and it is also empty for a drag that only touched one slot: the game hands",
    "that one over as an ordinary click, so a script is never asked to tell the two apart. `on menu interact`",
    "runs once per touched slot, so this is what says that those runs were one drag."
)
@Examples(
    "on menu interact:",
    "    loop the dragged slots:",
    "        send \"The drag reached slot %loop-value%.\" to player"
)
@Since("2.0.0")
class ExprEventDraggedSlots : SimpleExpression<Number>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.expression(
                addon,
                ExprEventDraggedSlots::class.java,
                Number::class.java,
                "[the] drag[ged] slots"
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

    override fun get(event: Event?): Array<Number> {
        val interact = event as? MenuInteractEvent ?: return emptyArray()
        if (interact.slots.size < 2) return emptyArray()
        return interact.slots.map { it as Number }.toTypedArray()
    }

    override fun toString(event: Event?, debug: Boolean) = "the dragged slots"

    override fun isSingle() = false

    override fun getReturnType() = Number::class.java
}
