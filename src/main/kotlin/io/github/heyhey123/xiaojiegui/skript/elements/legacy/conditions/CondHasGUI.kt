package io.github.heyhey123.xiaojiegui.skript.elements.legacy.conditions

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Condition
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.bukkit.entity.Player
import org.bukkit.event.Event


@Name("Has GUI")
@Description(
    "Checks whether the given player(s) has/have a GUI open.",
    "Deprecated: Use ExprGetPlayerSession and check for null instead."
)
@Examples(
    "command /guiviewers: # Returns a list of all players with a GUI open.",
    "\tset {_viewers::*} to all players where [input has a gui]",
    "\tsend \"GUI Viewers: %{_viewers::*}%\" to player"
)
@Since("1.0.0-SNAPSHOT")
@Deprecated("Use ExprGetPlayerSession and check for null instead")
class CondHasGUI : Condition() {
    private lateinit var players: Expression<Player?>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        exprs: Array<Expression<*>?>,
        matchedPattern: Int,
        kleenean: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        players = exprs[0] as Expression<Player?>
        isNegated = matchedPattern == 1
        return true
    }

    override fun check(e: Event?): Boolean =
        players.check(
            e,
            { p ->
                p?.let { MenuSession.querySession(it) }?.menu != null
            }, isNegated
        )


    override fun toString(e: Event?, debug: Boolean): String {
        return players.toString(e, debug) + (if (!isNegated) " has/have " else " do not/don't have ") + " a gui open"
    }

    companion object {
        init {
            Skript.registerCondition(
                CondHasGUI::class.java,
                "%players% (has|have) a gui [open]",
                "%players% (doesn't|does not|do not|don't) have a gui [open]"
            )
        }
    }
}
