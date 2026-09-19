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
@Description(
    "All players currently viewing a menu.",
    "The property is called `menu viewers` rather than `viewers` because Skript 2.16 registers a `viewer[s]`",
    "property of its own over *any* object, and it is registered before this addon's syntax, so",
    "`the viewers of {_menu}` is answered by Skript and returns nothing. `all players viewing {_menu}` is the",
    "same value under a name Skript does not claim."
)
@Examples(
    "broadcast \"There are currently %size of the menu viewers of {_menu}% players viewing the menu.\"",
    "loop the menu viewers of {_menu}:",
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
                // No `the viewers of %menu%` pattern: see the description. `[the] menu viewers of %menu%`
                // deliberately has no `menu` literal before the slot, because a `%menu%` slot preceded by
                // the word `menu` parses `{_menu}` as a lookup by id, which finds nothing.
                "[the] menu viewers of %menu%",
                "%menu%'[s] menu viewers",
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
        "the menu viewers of ${menuExpr.toString(event, debug)}"

    override fun isSingle() = false

    override fun getReturnType() = Player::class.java
}
