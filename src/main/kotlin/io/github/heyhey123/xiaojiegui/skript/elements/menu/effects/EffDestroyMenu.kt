package io.github.heyhey123.xiaojiegui.skript.elements.menu.effects

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import org.bukkit.event.Event

@Name("Destroy Menu")
@Description(
    "Destroy the given menu.",
    "A destroyed menu cannot be opened or interacted with.",
    "If a menu is destroyed while players have it open, their menus will be forcibly closed.",
    "If the menu has been destroyed already, this effect does nothing."
)
@Examples(
    "destroy the menu menu with id \"main_menu\""
)
@Since("1.0-SNAPSHOT")
class EffDestroyMenu: Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffDestroyMenu::class.java,
                "destroy [the] menu %menu%"
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
        return true
    }

    override fun execute(event: Event?) {
        val menu = exprMenu.getSingle(event) ?: return
        menu.destroy()
    }

    override fun toString(event: Event?, debug: Boolean) =
        "destroy menu ${exprMenu.toString(event, debug)}"
}
