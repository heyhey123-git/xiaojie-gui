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
import org.bukkit.inventory.ItemStack
import org.skriptlang.skript.addon.SkriptAddon

@Name("Clicked Icon")
@Description("The icon in the slot the player clicked, in `on menu interact`.")
@Examples(
    "on menu interact:",
    "    send \"You clicked %the clicked icon%.\" to player"
)
@Since("1.0.0")
class ExprEventIcon : SimpleExpression<ItemStack>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.expression(
                addon,
                ExprEventIcon::class.java,
                ItemStack::class.java,
                // `clicked icon` first: it says what the value is without leaning on the `event-`
                // prefix, which a script rarely writes. The shorter forms stay for compatibility.
                "[the] clicked icon",
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
        val e = event as? MenuInteractEvent ?: return emptyArray()
        return arrayOf(e.icon)
    }

    override fun toString(event: Event?, debug: Boolean) = "the clicked icon"

    override fun isSingle() = true

    override fun getReturnType() = ItemStack::class.java
}
