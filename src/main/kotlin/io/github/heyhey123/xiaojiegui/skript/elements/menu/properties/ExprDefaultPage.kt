package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.classes.Changer
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import org.bukkit.event.Event

@Name("Default Page")
@Description("The default page to show a menu when opened.")
@Examples(
    "set {_page} to the default page of menu {_menu}",
    "set the default page of menu {_menu} to 2"
)
@Since("1.0-SNAPSHOT")
class ExprDefaultPage : SimplePropertyExpression<Menu, Number>() {

    companion object {
        init {
            register(
                ExprDefaultPage::class.java,
                Number::class.java,
                "default page",
                "menu"
            )
        }
    }

    override fun convert(from: Menu?): Number? = from?.properties?.defaultPage

    override fun getPropertyName() = "default page"

    override fun getReturnType() = Number::class.java

    override fun acceptChange(mode: Changer.ChangeMode?): Array<out Class<*>?>? =
        if (mode == Changer.ChangeMode.SET) arrayOf(Number::class.java) else emptyArray()

    override fun change(event: Event?, delta: Array<out Any?>?, mode: Changer.ChangeMode?) {
        if (mode != Changer.ChangeMode.SET) return
        val menu = expr.getSingle(event) ?: return
        val newPage = delta?.get(0) ?: return
        if (newPage !is Number) return
        menu.properties.defaultPage = newPage.toInt().coerceAtLeast(0)
    }
}
