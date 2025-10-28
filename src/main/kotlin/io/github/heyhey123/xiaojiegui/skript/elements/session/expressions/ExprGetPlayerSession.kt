package io.github.heyhey123.xiaojiegui.skript.elements.session.expressions

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.bukkit.entity.Player

@Name("Get Player's Menu Session")
@Description(
    "Get the menu session of a player.",
    "Returns null if the player does not have an active menu session."
)
@Examples(
    "set {_session} to menu session of player",
    "if {_session} is not set:",
    "send \"You are not in a menu!\" to player"
)
@Since("1.0-SNAPSHOT")
class ExprGetPlayerSession : SimplePropertyExpression<Player, MenuSession>() {

    companion object {
        init {
            register(
                ExprGetPlayerSession::class.java,
                MenuSession::class.java,
                "menu session",
                "player"
            )
        }
    }

    override fun convert(from: Player?): MenuSession? =
        from?.let { MenuSession.querySession(from) }

    override fun getPropertyName() = "menu session"

    override fun getReturnType() = MenuSession::class.java
}
