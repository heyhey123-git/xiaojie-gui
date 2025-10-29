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
    "set {_id} to the id of menu {_menu}",
    "send \"The menu id is %{_id}%\" to player"
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
