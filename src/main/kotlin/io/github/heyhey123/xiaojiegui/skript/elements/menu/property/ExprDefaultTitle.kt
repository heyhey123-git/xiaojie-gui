package io.github.heyhey123.xiaojiegui.skript.elements.menu.property

import ch.njol.skript.classes.Changer
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.ComponentHelper
import org.bukkit.event.Event

@Name("Default Title")
@Description(
    "The default title of a menu (when add a new page).",
    "It can be a string or a text component in skbee(if exists).",
)
@Examples("set the default title of menu with id \"main_menu\" to \"New Page\"")
@Since("1.0-SNAPSHOT")
class ExprDefaultTitle : SimplePropertyExpression<Menu, Any>() {

    companion object {
        init {

            @Suppress("UNCHECKED_CAST")
            register(
                ExprDefaultTitle::class.java,
                ComponentHelper.componentWrapperType as Class<Any>,
                "default title",
                "menu"
            )
        }
    }

    override fun convert(from: Menu?): Any? =
        from?.properties?.defaultTitle?.let { ComponentHelper.wrapComponent(it) }

    override fun getPropertyName() =
        "default title"

    override fun getReturnType() = ComponentHelper.componentWrapperType

    override fun acceptChange(mode: Changer.ChangeMode?): Array<out Class<*>?>? =
        if (mode == Changer.ChangeMode.SET) arrayOf(ComponentHelper.componentWrapperType) else null

    override fun change(event: Event?, delta: Array<out Any?>?, mode: Changer.ChangeMode?) {
        if (mode != Changer.ChangeMode.SET) return
        val menu = expr.getSingle(event) ?: return
        val newTitle = delta?.get(0) ?: return
        val component = ComponentHelper.extractComponent(newTitle, event) ?: return
        menu.properties.defaultTitle = component
    }
}
