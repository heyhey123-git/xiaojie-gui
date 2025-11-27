package io.github.heyhey123.xiaojiegui.skript.elements.session.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession

@Name("Menu Session Page")
@Description(
    "Get the current page index of a menu session.",
    "This expression returns the current page index of the specified menu session."
)
@Examples(
    "set {_page} to page of player's menu session",
    "send \"You are currently on page %{_page}% of the menu.\" to player"
)
@Since("1.0.4")
class ExprSessionPage: SimplePropertyExpression<MenuSession, Number>() {

    companion object {
        init {
            register(
                ExprSessionPage::class.java,
                Number::class.java,
                "page",
                "menusession"
            )
        }
    }

    override fun convert(session: MenuSession): Number = session.page

    override fun getPropertyName(): String = "page"

    override fun getReturnType() = Number::class.java

}
