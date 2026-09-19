package io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon
import org.skriptlang.skript.registration.SyntaxInfo

@Name("Get Menu by ID")
@Description(
    "Get a menu by its ID.",
    "The ID is the one defined when creating the menu."
)
@Examples(
    "set {_menu} to menu with id \"main_menu\"",
    "if {_menu} is not set:",
    "    send \"Menu with ID 'main_menu' does not exist.\" to player"
)
@Since("1.0.0")
class ExprMenuById : SimpleExpression<Menu>() {
    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.expression(
                addon,
                ExprMenuById::class.java,
                Menu::class.java,
                "[the] (menu|gui) [with [the] id] %string%",
                priority = SyntaxInfo.COMBINED
            )
        }
    }

    private var idExpr: Expression<String>? = null

    override fun get(event: Event?): Array<out Menu?> {
        val id = idExpr?.getSingle(event) ?: return emptyArray()
        val menu = Menu.menusWithId[id] ?: return emptyArray()
        return arrayOf(menu)
    }

    override fun toString(event: Event?, debug: Boolean): String = "menu with id ${idExpr?.toString(event, debug)}"

    override fun isSingle() = true

    override fun getReturnType() = Menu::class.java

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        idExpr = expressions?.get(0) as Expression<String>
        return true
    }
}
