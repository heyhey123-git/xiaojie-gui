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

@Name("Menu List Index")
@Description(
    "The 1-based position of the clicked slot in the page's list, in `on menu interact`.",
    "It is empty when the clicked slot is not one of the list slots, which is how a click on a button or on",
    "another key is told apart from a click on a result.",
    "1-based like every other number a script sees here, so it indexes a Skript list directly:",
    "`{results::%the menu list index%}`."
)
@Examples(
    "on menu interact:",
    "    set {_picked} to {results::%the menu list index%}"
)
@Since("2.0.0")
class ExprMenuListIndex : SimpleExpression<Number>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.expression(
                addon,
                ExprMenuListIndex::class.java,
                Number::class.java,
                "[the] menu list index"
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
        val menu = interact.session.menu ?: return emptyArray()
        if (interact.session.page < 1) return emptyArray()
        val index = menu.pages[interact.session.page].listIndex(interact.slot) ?: return emptyArray()
        return arrayOf(index)
    }

    override fun toString(event: Event?, debug: Boolean) = "the menu list index"

    override fun isSingle() = true

    override fun getReturnType() = Number::class.java
}
