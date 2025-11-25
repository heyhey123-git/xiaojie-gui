package io.github.heyhey123.xiaojiegui.skript.elements.menu.conditions

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.lang.Condition
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import org.bukkit.event.Event

@Name("Player Inventory Hidden")
@Description(
    "Checks whether the player inventory section in a menu is hidden or visible.")
@Examples(
    "if player inventory of {_menu} is hidden:",
    "    send \"The player inventory is hidden in this menu.\" to player"
)
class CondPlayerInvHidden: Condition() {

    companion object {
        init {
            Skript.registerCondition(
                CondPlayerInvHidden::class.java,
                "player inventory of %menu% is hidden",
                "player inventory of %menu% is(n't|is not) hidden"
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
        menuExpr = expressions!![0] as Expression<Menu>
        isNegated = matchedPattern == 1
        return true
    }

    override fun check(event: Event?): Boolean {
        val menu = menuExpr.getSingle(event)
        check(menu != null) {
            "Menu to check player inventory visibility cannot be null."
        } // When null, we shouldn't return true or false, but rather indicate an error.

        val isHidden = menu.properties.hidePlayerInventory
        return isNegated xor isHidden
    }

    override fun toString(event: Event?, debug: Boolean) =
        "player inventory of ${menuExpr.toString(event, debug)} " +
                if (!isNegated) "is hidden" else "is not hidden"
}
