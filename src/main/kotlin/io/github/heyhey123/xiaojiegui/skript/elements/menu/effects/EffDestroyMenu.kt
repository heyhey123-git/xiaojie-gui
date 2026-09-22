package io.github.heyhey123.xiaojiegui.skript.elements.menu.effects

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Destroy Menu")
@Description(
    "Destroy the given menu.",
    "A destroyed menu cannot be opened or interacted with.",
    "If a menu is destroyed while players have it open, their menus will be forcibly closed.",
    "If the menu has been destroyed already, this effect does nothing."
)
@Examples(
    "destroy the menu {_menu}",
    "destroy the menu with id \"main_menu\""
)
@Since("1.0.0")
class EffDestroyMenu : Effect() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.effect(
                addon,
                EffDestroyMenu::class.java,
                "destroy [the] menu %menu%",
                // A separate form for the id lookup: a pattern that has already consumed the word `menu`
                // cannot let `%menu%` start at `with id`, so without this a user has to write
                // `destroy the menu menu with id "x"` —the same word twice.
                "destroy [the] menu with id %string%"
            )
        }
    }

    private var menuExpr: Expression<Menu>? = null

    private var idExpr: Expression<String>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        if (matchedPattern == 0) {
            menuExpr = expressions!![0] as Expression<Menu>
        } else {
            idExpr = expressions!![0] as Expression<String>
        }
        return true
    }

    override fun execute(event: Event?) {
        val menu = menuExpr?.getSingle(event)
            ?: idExpr?.getSingle(event)?.let { Menu.menusWithId[it] }
        if (menu == null) {
            this.error("Menu to destroy cannot be null.")
            return
        }

        menu.destroy()
    }

    override fun toString(event: Event?, debug: Boolean) =
        "destroy menu ${menuExpr?.toString(event, debug) ?: idExpr?.toString(event, debug)}"
}
