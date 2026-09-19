package io.github.heyhey123.xiaojiegui.skript.elements.session.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.entity.Player
import org.skriptlang.skript.addon.SkriptAddon

@Name("Menu Session's Viewer")
@Description(
    "Get the viewer (player) of a menu session.",
    "Returns null if the session is invalid."
)
@Examples(
    // The session slot is an expression: `menu session {_session}` is not one, `{_session}` is.
    "set {_viewer} to the viewer of {_session}",
    "send \"You are viewing a menu\" to {_viewer}"
)
@Since("1.0.0")
class ExprSessionViewer : SimplePropertyExpression<MenuSession, Player>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.property(
                addon,
                ExprSessionViewer::class.java,
                Player::class.java,
                "viewer",
                "menusession"
            )
        }
    }

    override fun convert(from: MenuSession?): Player? = from?.viewer

    override fun getPropertyName() = "viewer"

    override fun getReturnType() = Player::class.java
}
