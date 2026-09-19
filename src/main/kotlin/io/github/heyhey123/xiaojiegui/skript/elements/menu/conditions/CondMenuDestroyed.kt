package io.github.heyhey123.xiaojiegui.skript.elements.menu.conditions

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Condition
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

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
@Since("1.0.0")
class CondMenuDestroyed : Condition() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.condition(
                addon,
                CondMenuDestroyed::class.java,
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
        val menu = exprMenu.getSingle(event) ?: return false
        val destroyed = menu.isDestroyed
        return isNegated xor destroyed
    }

    override fun toString(event: Event?, debug: Boolean) =
        "${exprMenu.toString(event, debug)} ${if (!isNegated) "is" else "is not"} destroyed"
}
