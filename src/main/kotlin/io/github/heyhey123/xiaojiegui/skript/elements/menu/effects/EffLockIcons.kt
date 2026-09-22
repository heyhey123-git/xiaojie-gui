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
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Lock Icons")
@Description(
    "Lock or unlock the slots a menu's pages give an icon to, which is the same switch `with locked icons`",
    "sets when the menu is created.",
    "Locked slots cannot be taken from, put into, swapped, dropped or collected from, and a drag that",
    "touches one is refused as a whole; the slots a layout leaves empty stay the player's.",
    "This changes what happens to the *next* interaction, so nothing on screen changes now."
)
@Examples(
    "edit menu with id \"main_menu\":",
    "    lock the icons of the menu with id \"main_menu\""
)
@Since("2.0.0")
class EffLockIcons : Effect() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.effect(
                addon,
                EffLockIcons::class.java,
                "lock [the] icons of %menu%",
                "unlock [the] icons of %menu%"
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
            this.error("Menu whose icons to lock cannot be null.")
            return
        }

        menu.properties.lockedIcons = !isNegated

        // Locking is the same no-op `with locked icons` is on a phantom menu: nothing in that window can be
        // moved, so there is nothing to lock. Only the direction that promises protection is worth a line.
        if (!isNegated && menu.properties.mode == Receptacle.Mode.PHANTOM) {
            warning(
                "\"lock the icons of %menu%\" does nothing on a phantom menu: nothing in a phantom window " +
                    "can be moved, in or out, so there is nothing to lock. The flag only affects static menus."
            )
        }
    }

    override fun toString(event: Event?, debug: Boolean) =
        (if (!isNegated) "lock" else "unlock") +
            " the icons of ${menuExpr.toString(event, debug)}"
}
