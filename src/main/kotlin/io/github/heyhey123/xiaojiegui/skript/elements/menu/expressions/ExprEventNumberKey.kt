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

@Name("Pressed Number Key")
@Description(
    "The number key the player pressed in `on menu interact`, 1 to 9.",
    "It is nothing for every other click. Skript's click type reports a single `number key` for all",
    "nine keys, so this is how a menu tells 1 apart from 9 -- for example to use the keys as shortcuts."
)
@Examples(
    "on menu interact:",
    "    if the pressed number key is 1:",
    "        send \"You pressed 1!\" to player"
)
@Since("2.0.0")
class ExprEventNumberKey : SimpleExpression<Number>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.expression(
                addon,
                ExprEventNumberKey::class.java,
                Number::class.java,
                // No `[event-]` variant: `the pressed number key` is already a phrase a script says,
                // and `the event-pressed number key` is not one.
                "[the] pressed number key"
            )
        }
    }

    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean = parser.isCurrentEvent(MenuInteractEvent::class.java)

    override fun get(event: Event?): Array<Number> {
        val e = event as? MenuInteractEvent ?: return emptyArray()
        return e.clickType.numberKey?.let { arrayOf<Number>(it) } ?: emptyArray()
    }

    override fun toString(event: Event?, debug: Boolean) = "the pressed number key"

    override fun isSingle() = true

    override fun getReturnType() = Number::class.java
}
