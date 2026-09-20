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

@Name("Hide Player Inventory")
@Description(
    "Hide or show the player inventory section in a menu.",
    "Showing it is refused for a menu whose page lays the rows below the container out (a `player layout`): " +
        "those rows are the menu's own space there, and a window cannot be half the page's and half the " +
        "player's. Build the menu without a player layout to give the player that half back."
)
@Examples(
    "hide player inventory of {_menu}",
    "show player inventory of {_menu}"
)
@Since("1.0.0")
class EffHidePlayerInv : Effect() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.effect(
                addon,
                EffHidePlayerInv::class.java,
                "hide player inventory of %menu%",
                "show player inventory of %menu%"
            )
        }
    }

    private lateinit var menuExpr: Expression<Menu>

    private var isNegated = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        menuExpr = expressions!![0] as Expression<Menu>
        isNegated = matchedPattern == 1
        return true
    }

    override fun execute(event: Event?) {
        val menu = menuExpr.getSingle(event)
        if (menu == null) {
            error("Menu to set player inventory visibility cannot be null.")
            return
        }

        // The rows below the container are the menu's own space in a menu whose page lays them out, and the
        // two states cannot both be had: a window that is half the page's icons and half the player's items
        // is the one window this addon does not build. So showing the inventory is refused here, with a line
        // that says what to do instead, rather than producing that window or quietly ignoring the request.
        if (isNegated && menu.pages.any { it.hasPlayerLayout }) {
            warning(
                "\"show player inventory\" cannot undo the player layout of this menu: the rows below the " +
                    "container are that page's own space. Destroy the menu and build it again without a " +
                    "player layout to give the player that half back."
            )
            return
        }

        menu.properties.hidePlayerInventory = !isNegated
    }

    override fun toString(event: Event?, debug: Boolean) =
        (if (!isNegated) "hide" else "show") +
            " player inventory of ${menuExpr.toString(event, debug)}"
}
