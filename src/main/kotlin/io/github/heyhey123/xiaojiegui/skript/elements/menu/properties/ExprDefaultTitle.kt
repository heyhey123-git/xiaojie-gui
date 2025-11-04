package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.utils.ComponentHelper
import org.bukkit.event.Event

@Name("Default Title")
@Description(
    "The default title of a menu (when add a new page).",
    "It can be a string or a text component in skbee(if exists).",
)
@Examples(
    "set {_title} to the default title of menu {_menu}",
    "set the default title of menu {_menu} to \"&aMy Menu\""
)
@Since("1.0-SNAPSHOT")
class ExprDefaultTitle : SimplePropertyExpression<Menu, Any>() {

    companion object {
        init {

            @Suppress("UNCHECKED_CAST")
            register(
                ExprDefaultTitle::class.java,
                ComponentHelper.titleReturnType as Class<Any>,
                "default title",
                "menu"
            )
        }
    }

    override fun convert(from: Menu?): Any? =
        from?.properties?.defaultTitle?.let { ComponentHelper.wrapComponentOrString(it) }

    override fun getPropertyName() =
        "default title"

    override fun getReturnType() = ComponentHelper.titleReturnType

    override fun acceptChange(mode: Changer.ChangeMode?): Array<out Class<*>?> =
        if (mode == Changer.ChangeMode.SET) ComponentHelper.titleReturnTypes
        else arrayOf()

    override fun change(event: Event?, delta: Array<out Any?>?, mode: Changer.ChangeMode?) {
        if (mode != Changer.ChangeMode.SET) return
        val menu = expr.getSingle(event) ?: return
        val titleArg = delta?.get(0)
        if (titleArg == null) {
            Skript.error("Title cannot be null")
            return
        }

        val title = ComponentHelper.extractComponentOrNull(titleArg)
        if (title == null) {
            Skript.error("Valid title required.")
            return
        }

        menu.properties.defaultTitle = title
    }
}
