package io.github.heyhey123.xiaojiegui.skript.elements.session.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession


@Name("Session's Menu")
@Description(
    "Get the menu of a menu session.",
    "Returns null if the session is invalid."
)
@Examples(
    "set {_menu} to the menu of menu session of player",
    "if the menu of player's current menu session is not null:",
    "\tsend \"You have a menu open!\" to player"
)
@Since("1.0-SNAPSHOT")
class ExprSessionMenu : SimplePropertyExpression<MenuSession, Menu>() {

    companion object {
        init {
            register(
                ExprSessionMenu::class.java,
                Menu::class.java,
                "menu",
                "menusession"
            )
        }
    }

    override fun convert(from: MenuSession?): Menu? = from?.menu

    override fun getPropertyName() = "menu"

    override fun getReturnType() = Menu::class.java

}
