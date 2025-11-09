package io.github.heyhey123.xiaojiegui.skript.elements.menu.conditions

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Condition
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import org.bukkit.event.Event

@Name("Menu is Destroyed")
@Description(
    "Checks whether the given menu is destroyed.",
    "A destroyed menu cannot be opened or interacted with.",
    "Menus are usually destroyed when a new menu with the same ID is created, or when the server shuts down."
)
@Examples(
    "if the menu with id \"main_menu\" is destroyed:",
    "    send \"The main menu has been destroyed and can no longer be used.\" to player"
)
@Since("1.0-SNAPSHOT")
class CondDestroyed: Condition() {

    companion object {
        init {
            Skript.registerCondition(
                CondDestroyed::class.java,
                "[the] [menu] %menu% (is|was) destroyed",
                "[the] [menu] %menu% (isn't|is not|wasn't|was not) destroyed"
            )
        }
    }

    private lateinit var exprMenu: Expression<Menu>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        exprMenu = expressions!![0] as Expression<Menu>
        isNegated = matchedPattern == 1
        return true
    }

    override fun check(event: Event?): Boolean {
        val menu = exprMenu.getSingle(event) ?: return isNegated
        val destroyed = menu.isDestroyed
        return if (isNegated) !destroyed else destroyed
    }

    override fun toString(event: Event?, debug: Boolean) =
        "${exprMenu.toString(event, debug)} ${if (!isNegated) "is" else "is not"} destroyed"
}
