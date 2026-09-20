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
import org.bukkit.inventory.ItemStack
import org.skriptlang.skript.addon.SkriptAddon

@Name("Menu Contents")
@Description(
    "Everything a menu session's container holds, in slot order, with the empty slots left out.",
    "The slots are `the occupied slots of %menusession%`, so the two together are a snapshot that can be",
    "put back slot by slot. Only the container is read: a static menu's player half belongs to the player.",
    "The name carries `menu` because Skript's own `contents of %inventory%` is registered before this addon",
    "and would answer a shorter form for any variable, which is the same trap `menu viewers` exists for."
)
@Examples(
    "on menu close:",
    "    set {_contents::*} to the menu contents of the menu session of player",
    "    send \"You left %size of {_contents::*}% item(s) behind.\" to player"
)
@Since("2.0.0")
class ExprSessionContents : SimpleExpression<ItemStack>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.expression(
                addon,
                ExprSessionContents::class.java,
                ItemStack::class.java,
                "[the] menu contents of %menusession%"
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

    override fun get(event: Event?): Array<ItemStack> {
        val session = sessionExpr.getSingle(event) ?: return emptyArray()
        val size = session.receptacle?.layout?.containerSize ?: return emptyArray()
        return (0 until size).mapNotNull { session.getIcon(it) }.toTypedArray()
    }

    override fun toString(event: Event?, debug: Boolean) =
        "the menu contents of ${sessionExpr.toString(event, debug)}"

    override fun isSingle() = false

    override fun getReturnType() = ItemStack::class.java
}
