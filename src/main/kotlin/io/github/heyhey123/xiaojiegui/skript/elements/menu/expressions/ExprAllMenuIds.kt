package io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import org.bukkit.event.Event

@Name("Get All Menu IDs")
@Description(
    "Get all menu IDs.",
    "This expression returns a list of all menu IDs that have been created.",
    "Tips: This expression only returns the ids of menus have ids."
)
@Examples(
    "set {_menuIds::*} to all menu ids",
    "send \"Available menus: %{_menuIds::*}%\" to player"
)
@Since("1.0-SNAPSHOT")
class ExprAllMenuIds : SimpleExpression<String>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprAllMenuIds::class.java,
                String::class.java,
                ExpressionType.SIMPLE,
                "all [the] (menu|gui) id[s]"
            )
        }
    }

    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ) = true

    override fun get(event: Event?): Array<String> =
        Menu.menusWithId.keys.toTypedArray()

    override fun toString(event: Event?, debug: Boolean) = "all menu ids"

    override fun isSingle() = false

    override fun getReturnType() = String::class.java

}
