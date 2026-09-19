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

@Name("Player Inventory Hidden")
@Description(
    "Checks whether the player inventory section in a menu is hidden or visible."
)
@Examples(
    "if player inventory of {_menu} is hidden:",
    "    send \"The player inventory is hidden in this menu.\" to player"
)
@Since("1.0.0")
class CondPlayerInvHidden : Condition() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.condition(
                addon,
                CondPlayerInvHidden::class.java,
                "player inventory of %menu% is hidden",
                // "(isn't|is not)" and not "is(n't|is not)": the part outside the group is a literal,
                // so the second alternative would have to be spelled "is is not" to match.
                "player inventory of %menu% (isn't|is not) hidden"
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
        // An expression that yields nothing is ordinary in Skript, and the condition's answer to it is
        // false. Throwing here would abort the trigger instead of answering the question.
        val menu = menuExpr.getSingle(event) ?: return false

        val isHidden = menu.properties.hidePlayerInventory
        return isNegated xor isHidden
    }

    override fun toString(event: Event?, debug: Boolean) =
        "player inventory of ${menuExpr.toString(event, debug)} " +
            if (!isNegated) "is hidden" else "is not hidden"
}
