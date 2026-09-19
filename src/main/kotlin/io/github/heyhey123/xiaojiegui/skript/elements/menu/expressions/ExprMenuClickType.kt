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
import io.github.heyhey123.xiaojiegui.gui.interact.BukkitClickType
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Menu Click Type")
@Description(
    "The click that was made in `on menu interact`.",
    "It is Skript's click type, so it is written and compared with the words `on inventory click` uses:",
    "`left mouse button`, `left mouse button with shift`, `number key`, ...",
    "`the event-clicktype` is the same value through Skript's event-value form."
)
@Examples(
    "on menu interact:",
    "    if the click type is left mouse button:",
    "        send \"Left click!\" to player"
)
@Since("2.0.0")
class ExprMenuClickType : SimpleExpression<BukkitClickType>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.expression(
                addon,
                ExprMenuClickType::class.java,
                BukkitClickType::class.java,
                // Skript's own `the click type` belongs to `ExprClicked`, which refuses to parse outside
                // an inventory click event; this element answers for a menu interact event instead.
                "[the] click type"
            )
        }
    }

    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean = parser.isCurrentEvent(MenuInteractEvent::class.java)

    override fun get(event: Event?): Array<BukkitClickType> {
        val e = event as? MenuInteractEvent ?: return emptyArray()
        return e.clickType.bukkitClickType?.let { arrayOf(it) } ?: emptyArray()
    }

    override fun toString(event: Event?, debug: Boolean) = "the click type"

    override fun isSingle() = true

    override fun getReturnType() = BukkitClickType::class.java
}
