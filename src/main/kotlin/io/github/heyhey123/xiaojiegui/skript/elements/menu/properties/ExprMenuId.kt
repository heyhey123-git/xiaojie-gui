package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu


@Name("Menu ID")
@Description("Gets the ID of a menu.")
@Examples(
    "command /menuid:",
    "trigger:",
    "open menu with id \"main_menu\" to player",
    "send \"You opened the menu with ID: %menu id of player's open menu%\" to player"
)
@Since("1.0-SNAPSHOT")
class ExprMenuId : SimplePropertyExpression<Menu, String>() {

    companion object {
        init {
            register(
                ExprMenuId::class.java,
                String::class.java,
                "id",
                "menu"
            )
        }
    }

    override fun convert(from: Menu?) = from?.id

    override fun getPropertyName() = "id"

    override fun getReturnType() = String::class.java

}
