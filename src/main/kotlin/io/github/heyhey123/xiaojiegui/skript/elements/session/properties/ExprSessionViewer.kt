package io.github.heyhey123.xiaojiegui.skript.elements.session.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.bukkit.entity.Player


@Name("Menu Session's Viewer")
@Description(
    "Get the viewer (player) of a menu session.",
    "Returns null if the session is invalid."
)
@Examples(
    "set {_player} to viewer of menu session of player"
)
@Since("1.0-SNAPSHOT")
class ExprSessionViewer : SimplePropertyExpression<MenuSession, Player>() {

    companion object {
        init {
            register(
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
