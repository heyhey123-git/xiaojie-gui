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

@Name("Icons Locked")
@Description(
    "Checks whether the slots a menu's pages give an icon to are the menu's own, which is what `with locked",
    "icons` sets and what `lock the icons of %menu%` can turn on and off."
)
@Examples(
    "if the icons of {_menu} are locked:",
    "    send \"You cannot take anything out of this menu.\" to player"
)
@Since("2.0.0")
class CondIconsLocked : Condition() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.condition(
                addon,
                CondIconsLocked::class.java,
                // Both spellings start with `[the]`: Skript matches a pattern against the whole line, so a
                // pattern without it would refuse the `the icons of …` a script naturally writes.
                "[the] icons of %menu% (is|are) locked",
                "[the] icons of %menu% (isn't|is not|aren't|are not) locked"
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

        val isLocked = menu.properties.lockedIcons
        return isNegated xor isLocked
    }

    override fun toString(event: Event?, debug: Boolean) =
        "icons of ${menuExpr.toString(event, debug)} " +
            if (!isNegated) "are locked" else "are not locked"
}
