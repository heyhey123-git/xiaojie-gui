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
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Hide Player Inventory")
@Description("Hide or show the player inventory section in a menu.")
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
            Skript.error("Menu to set player inventory visibility cannot be null.")
            return
        }

        // Showing the inventory takes the rows below the container back for the player, which undoes what a
        // player layout asks for: that page's icons for those rows are then drawn over the player's own
        // items, and this addon builds no window that is half one and half the other. One line each time it
        // is asked for, rather than a menu that quietly becomes that.
        if (isNegated && menu.pages.any { it.hasPlayerLayout }) {
            Skript.warning(
                "\"show player inventory\" undoes the player layout of this menu: the rows below the " +
                    "container are the player's while its inventory is shown, so the icons the page lays " +
                    "out for them land on the player's own items."
            )
        }

        menu.properties.hidePlayerInventory = !isNegated
    }

    override fun toString(event: Event?, debug: Boolean) =
        (if (!isNegated) "hide" else "show") +
            " player inventory of ${menuExpr.toString(event, debug)}"
}
