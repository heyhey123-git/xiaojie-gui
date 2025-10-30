package io.github.heyhey123.xiaojiegui.skript.elements.menu.properties

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.util.SimpleExpression
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import org.bukkit.event.Event


@Name("Default Layout")
@Description("The default layout of a menu.")
@Examples(
    "set {_layout::*} to the default layout of menu {_menu}",
    "set the default layout of menu {_menu} to \"#########\", \"#.......#\", \"#..###..#\", \"#..###..#\", \"#.......#\", \"#########\""
)
@Since("1.0-SNAPSHOT")
class ExprDefaultLayout : SimpleExpression<String>() {
    companion object {
        init {
            Skript.registerExpression(
                ExprDefaultLayout::class.java,
                String::class.java,
                ExpressionType.PROPERTY,
                "[the] default layout of [the] [(menu|gui)] %menu%",
                "%menu%'[s] default layout",
            )
        }
    }

    private lateinit var menuExpr: Expression<Menu>

    override fun getReturnType() = String::class.java

    override fun isSingle() = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        exprs: Array<out Expression<*>>?,
        matchedPattern: Int,
        isDelayed: ch.njol.util.Kleenean?,
        parseResult: ch.njol.skript.lang.SkriptParser.ParseResult?
    ): Boolean {
        menuExpr = exprs?.get(0) as Expression<Menu>
        return true
    }

    override fun get(event: Event?): Array<String> {
        val menu = menuExpr.getSingle(event) ?: return arrayOf()
        return menu.properties.defaultLayout.toTypedArray()
    }

    override fun acceptChange(mode: Changer.ChangeMode?): Array<out Class<*>?> =
        if (mode == Changer.ChangeMode.SET) arrayOf(Array<String>::class.java)
        else arrayOf()

    @Suppress("UNCHECKED_CAST")
    override fun change(event: Event?, delta: Array<out Any?>?, mode: Changer.ChangeMode?) {
        if (mode != Changer.ChangeMode.SET) return
        val menu = menuExpr.getSingle(event)
        if (menu == null) {
            Skript.error("Menu cannot be null: ${this.toString(event, true)}")
            return
        }

        if (delta == null) {
            Skript.error("Layout cannot be null")
            return
        }

        if (delta.any { it !is String }) {
            Skript.error("All layout rows must be strings.")
            return
        }

        menu.properties.defaultLayout = delta.toList() as List<String>
    }

    override fun toString(event: Event?, debug: Boolean): String =
        "the default layout of the menu ${menuExpr.toString(event, debug)}"

}
