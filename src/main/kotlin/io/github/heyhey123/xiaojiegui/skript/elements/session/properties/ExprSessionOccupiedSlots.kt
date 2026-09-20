package io.github.heyhey123.xiaojiegui.skript.elements.session.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Occupied Slots")
@Description(
    "The slots of a menu session's container that hold something, in ascending order.",
    "This is the slot-aware half of reading a window: together with `icon in slot %number% of %menusession%`",
    "it is how a menu that keeps things in its free slots is saved and loaded, without needing to know which",
    "slots the player used.",
    "Only the container is read -- the slots the layout describes. A static menu's player half belongs to the",
    "player, not to the menu."
)
@Examples(
    "on menu close:",
    "    loop the occupied slots of the menu session of player:",
    "        set {backpack::%loop-value%} to icon in slot loop-value of the menu session of player"
)
@Since("2.0.0")
class ExprSessionOccupiedSlots : SimpleExpression<Number>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.expression(
                addon,
                ExprSessionOccupiedSlots::class.java,
                Number::class.java,
                "[the] occupied slots of %menusession%"
            )
        }
    }

    private lateinit var sessionExpr: Expression<MenuSession>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        sessionExpr = expressions?.get(0) as Expression<MenuSession>
        return true
    }

    override fun get(event: Event?): Array<Number> {
        val session = sessionExpr.getSingle(event) ?: return emptyArray()
        val size = session.receptacle?.layout?.containerSize ?: return emptyArray()
        return (0 until size).filter { session.getIcon(it) != null }.map { it as Number }.toTypedArray()
    }

    override fun toString(event: Event?, debug: Boolean) =
        "the occupied slots of ${sessionExpr.toString(event, debug)}"

    override fun isSingle() = false

    override fun getReturnType() = Number::class.java
}
