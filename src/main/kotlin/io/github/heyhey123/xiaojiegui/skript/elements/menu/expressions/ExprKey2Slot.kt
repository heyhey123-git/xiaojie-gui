package io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.event.MenuEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import org.bukkit.event.Event

@Name("Slot Key from Menu")
@Description(
    "The slot(s) of a specific key in a specific page of a menu.",
    "This expression returns the slot(s) associated with the specified key in the specified page of the given menu.",
    "If the key does not have any associated slots, it returns nothing."
)
@Examples(
    "set {_slots::*} to the slot of key \"example_key\" in page 0 of menu",
    "send \"The slots of key 'example_key' in page 0 are %{_slots::*}%\" to player"
)
@Since("1.0-SNAPSHOT")
class ExprKey2Slot : SimpleExpression<Number>() {
    companion object {
        init {
            Skript.registerExpression(
                ExprKey2Slot::class.java,
                Number::class.java,
                ExpressionType.COMBINED,
                "[the] slot[s] of [the] key %string% in [the] page %number% [of [the] menu %-menu%]"
            )
        }
    }

    private lateinit var keyExpr: Expression<String>

    private lateinit var pageExpr: Expression<Number>

    private var menuExpr: Expression<Menu>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        keyExpr = expressions!![0] as Expression<String>
        pageExpr = expressions[1] as Expression<Number>
        menuExpr = expressions[2] as Expression<Menu>?

        return menuExpr != null ||
                parser.isCurrentEvent(MenuEvent::class.java, ProvideMenuEvent::class.java)
    }

    override fun get(event: Event?): Array<Number?> {
        val key = keyExpr.getSingle(event) ?: return emptyArray()
        val page = pageExpr.getSingle(event)?.toInt() ?: return emptyArray()
        val menu = menuExpr?.getSingle(event) ?: when (event) {
            is MenuEvent -> event.menu
            is ProvideMenuEvent -> event.menu
            else -> {
                Skript.error(
                    "Cannot determine menu: no menu was provided " +
                            "and the current event is not a MenuEvent or ProvideMenuEvent."
                )
                return emptyArray()
            }
        }

        if (page !in 1..<menu.size) {
            Skript.error(
                "Page index $page is out of bounds " +
                        "for menu '${menuExpr?.toString(event, true) ?: "current menu"}' " +
                        "with ${menu.pages.size} pages."
            )
            return emptyArray()
        }

        val slots = menu.pages[page].keyToSlots[key]
        return slots?.toTypedArray() ?: emptyArray()
    }

    override fun acceptChange(mode: Changer.ChangeMode?): Array<Class<Number>> {
        return when (mode) {
            Changer.ChangeMode.ADD,
            Changer.ChangeMode.SET,
            Changer.ChangeMode.REMOVE,
            Changer.ChangeMode.RESET -> arrayOf(Number::class.java)

            else -> emptyArray()
        }
    }

    override fun change(event: Event?, delta: Array<out Any>?, mode: Changer.ChangeMode?) {
        val key = keyExpr.getSingle(event) ?: return
        val page = pageExpr.getSingle(event)?.toInt() ?: return
        val menu = menuExpr?.getSingle(event) ?: when (event) {
            is MenuEvent -> event.menu
            is ProvideMenuEvent -> event.menu
            else -> {
                Skript.error(
                    "Cannot determine menu: no menu was provided " +
                            "and the current event is not a MenuEvent or ProvideMenuEvent."
                )
                return
            }
        }

        if (page !in 1..<menu.size) {
            Skript.error(
                "Page index $page is out of bounds " +
                        "for menu '${menuExpr?.toString(event, true) ?: "current menu"}' " +
                        "with ${menu.pages.size} pages."
            )
            return
        }

        val pageInstance = menu.pages[page]
        when (mode) {
            Changer.ChangeMode.ADD -> {
                val slots = delta?.mapNotNull { it as? Number }?.map { it.toInt() } ?: return
                val slotSet = pageInstance.keyToSlots.computeIfAbsent(key) { mutableSetOf() }
                slotSet.addAll(slots)
            }

            Changer.ChangeMode.SET -> {
                val slots = delta?.mapNotNull { it as? Number }?.map { it.toInt() } ?: return
                val slotSet = pageInstance.keyToSlots.computeIfAbsent(key) { mutableSetOf() }
                slotSet.clear()
                slotSet.addAll(slots)
            }

            Changer.ChangeMode.REMOVE,
            Changer.ChangeMode.RESET -> {
                val slots = delta?.mapNotNull { it as? Number }?.map { it.toInt() } ?: return
                val slotSet = pageInstance.keyToSlots[key] ?: return
                slotSet.removeAll(slots.toSet())
            }

            else -> {
                Skript.error("Change mode $mode is not supported for slot of key expression.")
            }
        }
    }

    override fun toString(event: Event?, debug: Boolean) =
        "slot of key ${keyExpr.toString(event, debug)} in page ${pageExpr.toString(event, debug)} of menu ${
            menuExpr?.toString(event, debug)
                ?: "current menu"
        }"

    override fun isSingle() = false

    override fun getReturnType() = Number::class.java
}
