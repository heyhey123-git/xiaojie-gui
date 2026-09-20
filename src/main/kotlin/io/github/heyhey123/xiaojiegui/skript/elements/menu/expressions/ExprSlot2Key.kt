package io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions

import ch.njol.skript.classes.Changer
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.event.MenuEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon
import org.skriptlang.skript.registration.SyntaxInfo

@Name("Key of Slot")
@Description(
    "The key of a specific slot in a specific page of a menu.",
    "This expression returns the key associated with the specified slot in the specified page of the given menu.",
    "If the slot does not have an associated key, it returns nothing."
)
@Examples(
    // The menu slot after `of` takes an expression; the bare word `menu` is not one.
    "set {_key} to the key of slot 5 in page 1 of {_menu}",
    "send \"The key of slot 5 in page 1 is %{_key}%\" to player"
)
@Since("1.0.0")
class ExprSlot2Key : SimpleExpression<String>() {
    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.expression(
                addon,
                ExprSlot2Key::class.java,
                String::class.java,
                // `(menu|gui)` is optional inside this optional group so that the natural `of {_menu}`
                // works as well as `of the menu {_menu}` and `of menu with id "x"`.
                "[the] key of [the] slot %number% in [the] page %number% [of [the] [(menu|gui)] %-menu%]",
                priority = SyntaxInfo.COMBINED
            )
        }
    }

    private lateinit var slotExpr: Expression<Number>

    private lateinit var pageExpr: Expression<Number>

    private var menuExpr: Expression<Menu>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        slotExpr = expressions!![0] as Expression<Number>
        pageExpr = expressions[1] as Expression<Number>
        menuExpr = expressions[2] as Expression<Menu>?

        return menuExpr != null ||
            parser.isCurrentEvent(MenuEvent::class.java, ProvideMenuEvent::class.java)
    }

    override fun get(event: Event?): Array<out String?> {
        val slot = slotExpr.getSingle(event)?.toInt() ?: return emptyArray()
        val page = pageExpr.getSingle(event)?.toInt() ?: return emptyArray()
        val menu = menuExpr?.getSingle(event) ?: when (event) {
            is MenuEvent -> event.menu
            is ProvideMenuEvent -> event.menu
            else -> {
                error(
                    "Cannot determine menu: no menu was provided " +
                        "and the current event is not a MenuEvent or ProvideMenuEvent."
                )
                return emptyArray()
            }
        }

        if (page !in 1..menu.size) {
            error(
                "Page index $page is out of bounds " +
                    "for menu '${menuExpr?.toString(event, true) ?: "current menu"}' " +
                    "with ${menu.pages.size} pages."
            )
            return emptyArray()
        }

        val key = menu.pages[page].keyToSlots.entries.firstOrNull { slot in it.value }?.key
            ?: return emptyArray()
        return arrayOf(key)
    }

    override fun acceptChange(mode: Changer.ChangeMode?): Array<out Class<*>?>? = when (mode) {
        // Deleting and resetting carry no value, but Skript still dereferences every entry of this
        // array, so an entry of null throws inside the change effect. The declared type is what the
        // expression can hold, whether or not the mode uses a value.
        Changer.ChangeMode.DELETE,
        Changer.ChangeMode.RESET,
        Changer.ChangeMode.SET -> arrayOf(String::class.java)

        else -> emptyArray()
    }

    override fun change(event: Event?, delta: Array<out Any?>?, mode: Changer.ChangeMode?) {
        val slot = slotExpr.getSingle(event)?.toInt() ?: return
        val page = pageExpr.getSingle(event)?.toInt() ?: return
        val menu = menuExpr?.getSingle(event) ?: when (event) {
            is MenuEvent -> event.menu
            is ProvideMenuEvent -> event.menu
            else -> {
                error(
                    "Cannot determine menu: no menu was provided " +
                        "and the current event is not a MenuEvent or ProvideMenuEvent."
                )
                return
            }
        }

        if (page !in 1..menu.size) {
            error("Page index $page is out of bounds for the menu.")
            return
        }

        val keyToSlots = menu.pages[page].keyToSlots
        val entry = keyToSlots.entries.firstOrNull { slot in it.value }

        when (mode) {
            Changer.ChangeMode.RESET,
            Changer.ChangeMode.DELETE -> {
                if (entry == null) {
                    error("Slot $slot in page $page of menu has no associated key to remove.")
                    return
                }
                entry.value.remove(slot)
                if (entry.value.isEmpty()) {
                    keyToSlots.remove(entry.key)
                }
            }

            Changer.ChangeMode.SET -> {
                val newKey = delta?.get(0) as? String ?: return

                val keyToSlots = menu.pages[page].keyToSlots
                val existingEntry = keyToSlots.entries.firstOrNull { slot in it.value }
                existingEntry?.value?.remove(slot)
                if (existingEntry != null && existingEntry.value.isEmpty()) {
                    keyToSlots.remove(existingEntry.key)
                }

                // computeIfAbsent returns the existing set when the key is already mapped, so the slot
                // has to be added to the result rather than to a set built inside the lambda.
                keyToSlots.computeIfAbsent(newKey) { mutableSetOf() }.add(slot)
            }

            else -> error("Change mode $mode is not supported for this expression.")
        }
    }

    override fun toString(event: Event?, debug: Boolean) =
        "the key of slot ${slotExpr.toString(event, debug)} in page ${
            pageExpr.toString(
                event,
                debug
            )
        } of menu ${menuExpr?.toString(event, debug) ?: "current menu"}"

    override fun isSingle() = true

    override fun getReturnType() = String::class.java
}
