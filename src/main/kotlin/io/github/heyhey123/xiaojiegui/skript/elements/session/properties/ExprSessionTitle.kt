package io.github.heyhey123.xiaojiegui.skript.elements.session.properties

import ch.njol.skript.Skript
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
                ComponentHelper.titleReturnType as Class<Any>,
                "title",
                "menusession"
            )
        }
    }

    override fun convert(from: MenuSession?) =
        from?.receptacle?.title?.let {
            ComponentHelper.wrapComponentOrString(it)
        }

    override fun acceptChange(mode: Changer.ChangeMode?): Array<out Class<*>?> =
        if (mode == Changer.ChangeMode.SET) ComponentHelper.titleReturnTypes
        else arrayOf()

    override fun change(event: Event?, delta: Array<out Any?>?, mode: Changer.ChangeMode?) {
        if (mode != Changer.ChangeMode.SET) return
        val session = expr.getSingle(event)
        if (session == null) {
            Skript.error("Menu session cannot be null.")
            return
        }

        val titleArg = delta?.firstOrNull()
        if (titleArg == null) {
            Skript.error("Title cannot be null.")
            return
        }

        val title = ComponentHelper.extractComponentOrNull(titleArg)
        if (title == null) {
            Skript.error("Valid title required.")
            return
        }

        session.title(title, true)
    }

    override fun getPropertyName() = "title"

    override fun getReturnType() = ComponentHelper.titleReturnType

}
