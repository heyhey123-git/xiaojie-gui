package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.classes.Changer
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Default Page")
@Description("The default page to show a menu when opened.")
@Examples(
    // The property takes the menu expression directly: `of menu {_menu}` parses `menu {_menu}` as a
    // lookup by id instead.
    "set {_page} to the default page of {_menu}",
    "set the default page of {_menu} to 2"
)
@Since("1.0.0")
class ExprDefaultPage : SimplePropertyExpression<Menu, Number>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.property(
                addon,
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
        menu.properties.defaultPage = newPage.toInt().coerceAtLeast(1)
    }
}
