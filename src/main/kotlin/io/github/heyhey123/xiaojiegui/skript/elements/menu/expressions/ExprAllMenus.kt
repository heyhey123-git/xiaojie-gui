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

@Name("Get All Menus")
@Description(
    "Get all menus.",
    "This expression returns a list of all menus that have been created.",
    "Tips: This expression only returns the menus have ids."
)
@Examples(
    "set {_menus::*} to all menus",
    "loop {_menus::*}:",
    "    send \"Menu ID: %loop-value's id%\" to player"
)
@Since("1.0-SNAPSHOT")
class ExprAllMenus : SimpleExpression<Menu>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprAllMenus::class.java,
                Menu::class.java,
                ExpressionType.SIMPLE,
                "all [the] (menu|gui)[s]"
            )
        }
    }

    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean = true

    override fun get(event: Event?): Array<Menu> =
        Menu.menus.values.toTypedArray()

    override fun toString(event: Event?, debug: Boolean) =
        "all menus"

    override fun isSingle() = false

    override fun getReturnType() = Menu::class.java
}
