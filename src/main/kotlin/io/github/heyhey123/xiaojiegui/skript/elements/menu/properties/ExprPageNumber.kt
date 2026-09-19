package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.skriptlang.skript.addon.SkriptAddon

@Name("Page Number")
@Description("The total number of pages in a menu.")
@Examples(
    // The property takes the menu expression directly: after `of menu` the bare word `menu` would have
    // to be the expression, and no expression is named `menu`.
    "set {_pages} to the page number of {_menu}",
    "send \"This menu has %{_pages}% pages!\" to player"
)
@Since("1.0.0")
class ExprPageNumber : SimplePropertyExpression<Menu, Number>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.property(
                addon,
                ExprPageNumber::class.java,
                Number::class.java,
                "page number",
                "menu"
            )
        }
    }

    override fun convert(from: Menu?): Number? = from?.size

    override fun getPropertyName() = "page number"

    override fun getReturnType() = Number::class.java
}
