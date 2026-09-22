package io.github.heyhey123.xiaojiegui.skript.elements.session.effects

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import org.skriptlang.skript.addon.SkriptAddon

@Name("Set Menu List")
@Description(
    "Fill the list slots of a menu session's page with items, in order.",
    "The list slots are the slots of the key the page mapped a *list* of items to -- the key of",
    "`map key \"L\" to icon {_results::*}`, whose slots are in layout order. Extra items are dropped and",
    "slots with no item left are cleared, so one page can show a result of any length.",
    "The whole window is refreshed once, which is what makes this cheaper than writing the slots one by one:",
    "a browser filling 45 slots is one call and one update instead of 45 of each."
)
@Examples(
    "on menu interact:",
    "    set the menu list of the menu session of player to {results::*}"
)
@Since("2.0.0")
class EffSetMenuList : Effect() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.effect(
                addon,
                EffSetMenuList::class.java,
                "set [the] menu list of %menusession% to %itemstacks%"
            )
        }
    }

    private lateinit var sessionExpr: Expression<MenuSession>

    private lateinit var itemsExpr: Expression<ItemStack>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        sessionExpr = expressions!![0] as Expression<MenuSession>
        itemsExpr = expressions[1] as Expression<ItemStack>
        return true
    }

    override fun execute(event: Event?) {
        val session = sessionExpr.getSingle(event) ?: return
        val menu = session.menu
        if (menu == null || session.page < 1) {
            this.error("The menu session to fill a list in is not showing a menu.")
            return
        }

        if (!Bukkit.isPrimaryThread()) {
            this.error(
                "Menu lists can only be filled from the main server thread, " +
                    "but got called from an asynchronous thread: ${Thread.currentThread().name}\n" +
                    "current statement: ${this.toString(event, true)}"
            )
            return
        }

        menu.pages[session.page].setList(session, itemsExpr.getArray(event).toList())
    }

    override fun toString(event: Event?, debug: Boolean) =
        "set the menu list of ${sessionExpr.toString(event, debug)} to ${itemsExpr.toString(event, debug)}"
}
