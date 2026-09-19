package io.github.heyhey123.xiaojiegui.skript.elements.session.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.skriptlang.skript.addon.SkriptAddon

@Name("Session's Menu")
@Description(
    "Get the menu of a menu session.",
    "Returns null if the session is invalid."
)
@Examples(
    // `is not null` is not a Skript condition; a property that returns nothing is checked through a
    // variable with `is not set`.
    "set {_menu} to the menu of menu session of player",
    "if {_menu} is not set:",
    "\tsend \"You have no menu open!\" to player"
)
@Since("1.0.0")
class ExprSessionMenu : SimplePropertyExpression<MenuSession, Menu>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.property(
                addon,
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
