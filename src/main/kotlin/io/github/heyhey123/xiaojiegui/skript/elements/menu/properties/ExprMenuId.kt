package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.skriptlang.skript.addon.SkriptAddon

@Name("Menu ID")
@Description("Gets the ID of a menu.")
@Examples(
    // The property takes the menu expression directly: `of menu {_menu}` parses `menu {_menu}` as a
    // lookup by id instead of the menu whose id is asked for.
    "set {_id} to the id of {_menu}",
    "send \"The menu id is %{_id}%\" to player"
)
@Since("1.0.0")
class ExprMenuId : SimplePropertyExpression<Menu, String>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.property(
                addon,
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
