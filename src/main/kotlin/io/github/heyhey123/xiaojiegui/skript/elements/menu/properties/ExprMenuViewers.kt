package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event

@Name("Menu Viewers")
@Description("All players currently viewing a menu.")
@Examples("broadcast \"%all viewers of menu with id 'main_menu'%\"")
@Since("1.0-SNAPSHOT")
class ExprMenuViewers : SimpleExpression<Player>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprMenuViewers::class.java,
                Player::class.java,
                ExpressionType.PROPERTY,
                "[the] viewers of [the] [(menu|gui)] %menu%",
                "%menu%'[s] viewers",
                "[all] [(players|viewers)] viewing [the] [(menu|gui)] %menu%"
            )
        }
    }

    private lateinit var menu: Expression<Menu>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        menu = expressions?.get(0) as Expression<Menu>
        return true
    }

    override fun get(event: Event?): Array<out Player>? {
        val menu = menu.getSingle(event) ?: return null
        return menu.viewers.mapNotNull { uuid -> Bukkit.getPlayer(uuid) }.toTypedArray()
    }

    override fun toString(event: Event?, debug: Boolean): String =
        "the viewers of the menu ${menu.toString(event, debug)}"

    override fun isSingle() = false

    override fun getReturnType() = Player::class.java

}
