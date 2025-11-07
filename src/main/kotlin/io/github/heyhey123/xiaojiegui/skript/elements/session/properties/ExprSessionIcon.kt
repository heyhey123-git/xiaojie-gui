package io.github.heyhey123.xiaojiegui.skript.elements.session.properties

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
import io.github.heyhey123.xiaojiegui.XiaojieGUI.Companion.enableAsyncCheck
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack


@Name("Menu Session Icon")
@Description(
    "Get or set the icon(s) in specific slot(s) of a menu session.",
    "Returns null if the session is invalid or the slot is empty.",
    "When setting icons, all existing icons in the specified slots will be replaced.",
    "When deleting icons, all existing icons in the specified slots will be removed."
)
@Examples(
    "set {_icon} to icon of menu session {_session} in slot 10",
    "set icon in slot 5 of menu session {_session} to dirt",
    "delete icon in slot 3 of menu session {_session}"
)
@Since("1.0-SNAPSHOT")
class ExprSessionIcon : SimpleExpression<ItemStack>() {

    companion object {
        init {
            Skript.registerExpression(
                ExprSessionIcon::class.java,
                ItemStack::class.java,
                ExpressionType.COMBINED,
                "icon of %menusession% in slot %numbers%",
                "icon in slot %numbers% of %menusession%"
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

        if (enableAsyncCheck && !Bukkit.isPrimaryThread()) {
            Skript.error(
                "Menu session icons can only be modified from the main server thread, " +
                        "but got called from an asynchronous thread: ${Thread.currentThread().name}\n" +
                        "current statement: ${this.toString(event, true)}"
            )
        }

        when (mode) {
            Changer.ChangeMode.SET -> {
                val item = delta?.firstOrNull() as? ItemStack?
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

    override fun isSingle() = false

    override fun getReturnType() = ItemStack::class.java
}
