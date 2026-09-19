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

@Name("Cursor Item")
@Description(
    "The item the player was holding, in `on menu interact`.",
    "For a drag this is the stack it carried, which is what makes a drag easy to reason about: a drag only",
    "ever puts this item into slots, it never takes anything out.",
    "It is empty in phantom mode, where the window is the server's own and there is no real cursor."
)
@Examples(
    "on menu interact:",
    "    send \"You are holding %the cursor item%.\" to player"
)
@Since("2.0.0")
class ExprEventCursorItem : SimpleExpression<ItemStack>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.expression(
                addon,
                ExprEventCursorItem::class.java,
                ItemStack::class.java,
                "[the] cursor item"
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

    override fun get(event: Event?): Array<ItemStack> {
        val interact = event as? MenuInteractEvent ?: return emptyArray()
        return interact.cursor?.let { arrayOf(it) } ?: emptyArray()
    }

    override fun toString(event: Event?, debug: Boolean) = "the cursor item"

    override fun isSingle() = true

    override fun getReturnType() = ItemStack::class.java
}
