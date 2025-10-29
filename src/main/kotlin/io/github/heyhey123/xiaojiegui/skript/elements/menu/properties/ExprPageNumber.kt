package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu

@Name("Page Number")
@Description("The total number of pages in a menu.")
@Examples(
    "set {_pages} to the page number of menu",
    "send \"This menu has %{_pages}% pages!\" to player"
)
@Since("1.0-SNAPSHOT")
class ExprPageNumber : SimplePropertyExpression<Menu, Number>() {

    companion object {
        init {
            register(
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
