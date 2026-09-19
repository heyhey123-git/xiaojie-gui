package io.github.heyhey123.xiaojiegui.skript.elements.session.expressions

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.SimplePropertyExpression
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.entity.Player
import org.skriptlang.skript.addon.SkriptAddon

@Name("Get Player's Menu Session")
@Description(
    "Get the menu session of a player.",
    "Returns null if the player does not have an active menu session."
)
@Examples(
    "set {_session} to the menu session of player",
    "if {_session} is not set:",
    "\tsend \"You do not have an active menu session!\" to player"
)
@Since("1.0.0")
class ExprGetPlayerSession : SimplePropertyExpression<Player, MenuSession>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.property(
                addon,
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
