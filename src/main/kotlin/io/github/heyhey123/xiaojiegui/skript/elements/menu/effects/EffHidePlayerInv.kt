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

@Name("Hide Player Inventory")
@Description("Hide or show the player inventory section in a menu.")
@Examples(
    "hide player inventory of {_menu}",
    "show player inventory of {_menu}"
)
@Since("1.0-SNAPSHOT")
class EffHidePlayerInv: Effect() {

    companion object {
        init {
            Skript.registerEffect(
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

        menu.properties.hidePlayerInventory = !isNegated
    }

    override fun toString(event: Event?, debug: Boolean) =
        (if (!isNegated) "hide" else "show") +
                " player inventory of ${menuExpr.toString(event, debug)}"
}
