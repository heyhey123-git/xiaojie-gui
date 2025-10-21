package io.github.heyhey123.xiaojiegui.skript.elements.session.properties

import ch.njol.skript.classes.Changer
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.ComponentHelper
import org.bukkit.event.Event


@Name("Menu Session Title")
@Description(
    "Get or set the title of a menu session.",
    "Returns null if the session is invalid."
)
@Examples(
    "set {_title} to title of menu session of player",
    "set title of menu session of player to \"New Title\"",
    "set title of {_session} to \"Another Title\""
)
@Since("1.0-SNAPSHOT")
class ExprSessionTitle : SimplePropertyExpression<MenuSession, Any?>() {

    @Suppress("UNCHECKED_CAST")
    companion object {
        init {
            register(
                ExprSessionTitle::class.java,
                ComponentHelper.componentWrapperType::class.java as Class<Any>,
                "title",
                "menusession"
            )
        }
    }

    override fun convert(from: MenuSession?) =
        from?.receptacle?.title?.let {
            ComponentHelper.wrapComponent(it)
        }

    override fun acceptChange(mode: Changer.ChangeMode?): Array<out Class<*>?> =
        if (mode == Changer.ChangeMode.SET) arrayOf(ComponentHelper.componentWrapperType::class.java)
        else arrayOf()

    override fun change(event: Event?, delta: Array<out Any?>?, mode: Changer.ChangeMode?) {
        if (mode != Changer.ChangeMode.SET) return
        val session = expr.getSingle(event) ?: return
        val newTitle = delta?.get(0) ?: return
        val component = ComponentHelper.extractComponent(newTitle) ?: return
        session.title(component, true)
    }

    override fun getPropertyName() = "title"

    override fun getReturnType() = ComponentHelper.componentWrapperType

}
