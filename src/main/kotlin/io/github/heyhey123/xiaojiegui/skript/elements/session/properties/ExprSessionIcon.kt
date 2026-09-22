package io.github.heyhey123.xiaojiegui.skript.elements.session.properties

import ch.njol.skript.classes.Changer
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import org.skriptlang.skript.addon.SkriptAddon
import org.skriptlang.skript.registration.SyntaxInfo

@Name("Menu Session Icon")
@Description(
    "Get or set the icon(s) in specific slot(s) of a menu session.",
    "Returns null if the session is invalid or the slot is empty.",
    "When setting icons, all existing icons in the specified slots will be replaced.",
    "When deleting icons, all existing icons in the specified slots will be removed."
)
@Examples(
    "set {_icon} to icon in slot 10 of {_session}",
    "set {_icons::*} to icon in slot 5 and 10 of {_session}",
    "set icon in slot 5 of {_session} to dirt",
    "delete icon in slot 3 of {_session}"
)
@Since("1.0.0")
class ExprSessionIcon : SimpleExpression<ItemStack>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.expression(
                addon,
                ExprSessionIcon::class.java,
                ItemStack::class.java,
                "icon of %menusession% in slot %numbers%",
                "icon in slot %numbers% of %menusession%",
                priority = SyntaxInfo.COMBINED
            )
        }
    }

    private lateinit var sessionExpr: Expression<MenuSession>

    private lateinit var slotsExpr: Expression<Number>

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        when (matchedPattern) {
            0 -> {
                sessionExpr = expressions?.get(0) as Expression<MenuSession>
                slotsExpr = expressions[1] as Expression<Number>
            }

            1 -> {
                slotsExpr = expressions?.get(0) as Expression<Number>
                sessionExpr = expressions[1] as Expression<MenuSession>
            }
        }
        return true
    }

    override fun get(event: Event?): Array<ItemStack?> {
        val session = sessionExpr.getSingle(event) ?: return emptyArray()
        val slot = slotsExpr.getAll(event)
        return slot.map { session.getIcon(it.toInt()) }.toTypedArray()
    }

    override fun acceptChange(mode: Changer.ChangeMode?): Array<out Class<*>?> =
        when (mode) {
            Changer.ChangeMode.SET,
            Changer.ChangeMode.DELETE -> arrayOf(ItemStack::class.java)

            else -> emptyArray()
        }

    override fun change(event: Event?, delta: Array<out Any>?, mode: Changer.ChangeMode?) {
        val session = sessionExpr.getSingle(event) ?: return
        val slots = slotsExpr.getAll(event).map { it.toInt() }

        if (!Bukkit.isPrimaryThread()) {
            this.error(
                "Menu session icons can only be modified from the main server thread, " +
                    "but got called from an asynchronous thread: ${Thread.currentThread().name}\n" +
                    "current statement: ${this.toString(event, true)}"
            )
            return
        }

        when (mode) {
            Changer.ChangeMode.SET -> {
                // A SET always carries what to set. Reading a missing or wrong-typed value as null
                // would clear the slot instead of reporting the mistake.
                val item = delta?.firstOrNull() as? ItemStack
                if (item == null) {
                    this.error("Icon to set must be an item stack: ${this.toString(event, true)}")
                    return
                }
                session.setIcons(slots.associateWith { item }, true)
            }

            Changer.ChangeMode.DELETE -> {
                session.setIcons(slots.associateWith { null }, true)
            }

            else -> return
        }
    }

    override fun toString(event: Event?, debug: Boolean) =
        "icon in slot ${slotsExpr.toString(event, debug)} of ${sessionExpr.toString(event, debug)}"

    override fun isSingle() = slotsExpr.isSingle

    override fun getReturnType() = ItemStack::class.java
}
