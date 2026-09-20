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

@Name("Player's Menu Window")
@Description(
    "Get the menu window a player has open -- the page they are on, the icons they see, their title.",
    "Returns nothing if the player has no menu open.",
    "`window` is the word to reach for; `session` is this addon's own name for the same thing and works too."
)
@Examples(
    "set {_window} to the menu window of player",
    "if {_window} is not set:",
    "\tsend \"You do not have a menu open!\" to player"
)
@Since("1.0.0")
class ExprGetPlayerSession : SimplePropertyExpression<Player, MenuSession>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.property(
                addon,
                ExprGetPlayerSession::class.java,
                MenuSession::class.java,
                // Both words, so a script can say `the menu window of player` or `the menu session of
                // player` and read as itself. `window` is what the guide teaches.
                "menu (session|window)",
                "player"
            )
        }
    }

    override fun convert(from: Player?): MenuSession? =
        from?.let { MenuSession.querySession(from) }

    override fun getPropertyName() = "menu window"

    override fun getReturnType() = MenuSession::class.java
}
