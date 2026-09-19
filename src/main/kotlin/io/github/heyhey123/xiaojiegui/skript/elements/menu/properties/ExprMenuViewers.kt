package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.expressions.base.PropertyExpression
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Menu Viewers")
@Description("All players currently viewing a menu.")
@Examples(
    "broadcast \"There are currently %size of viewers of menu {_menu}% players viewing the menu.\"",
    "loop viewers of menu {_menu}:",
    "    send \"You are not alone! There are other players viewing this menu.\" to loop-player"
)
@Since("1.0.0")
class ExprMenuViewers : SimpleExpression<Player>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.expression(
                addon,
                ExprMenuViewers::class.java,
                Player::class.java,
                "[the] viewers of [the] [(menu|gui)] %menu%",
                "%menu%'[s] viewers",
                "[all] [(players|viewers)] viewing [the] [(menu|gui)] %menu%",
                priority = PropertyExpression.DEFAULT_PRIORITY
            )
        }
    }

    private lateinit var menuExpr: Expression<Menu>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        menuExpr = expressions?.get(0) as Expression<Menu>
        return true
    }

    override fun get(event: Event?): Array<out Player> {
        val menu = menuExpr.getSingle(event) ?: return emptyArray()
        return menu.viewers.mapNotNull { uuid -> Bukkit.getPlayer(uuid) }.toTypedArray()
    }

    override fun toString(event: Event?, debug: Boolean): String =
        "the viewers of the menu ${menuExpr.toString(event, debug)}"

    override fun isSingle() = false

    override fun getReturnType() = Player::class.java
}
