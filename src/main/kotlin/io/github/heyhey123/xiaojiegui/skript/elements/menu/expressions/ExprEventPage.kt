package io.github.heyhey123.xiaojiegui.skript.elements.menu.expressions

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.event.MenuOpenEvent
import io.github.heyhey123.xiaojiegui.gui.event.PageTurnEvent
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Event Page")
@Description(
    "The page a menu event happened on.",
    "`the future page` is the page a page turn is turning to; in `on menu open` and `on menu interact`",
    "there is no future page, so it returns nothing."
)
@Examples(
    "on page turn:",
    "    send \"Turning from page %the page% to page %the future page%.\" to player"
)
@Since("1.0.0")
class ExprEventPage : SimpleExpression<Number>() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.expression(
                addon,
                ExprEventPage::class.java,
                Number::class.java,
                "[the] [event-]page",
                "[the] future [event-]page"
            )
        }
    }

    private var isFuture: Boolean = false

    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        isFuture = matchedPattern == 1
        return parser.currentEvents?.any { c ->
            c == MenuOpenEvent::class.java ||
                c == MenuInteractEvent::class.java ||
                c == PageTurnEvent::class.java
        } ?: false
    }

    override fun get(event: Event?): Array<Number> =
        when (event) {
            // Only a page turn has a page it is turning away from and one it is turning to. In an
            // open or interact event there is no future page, and reporting the current one for it
            // would be an answer the script cannot tell apart from a real one.
            is MenuOpenEvent -> if (isFuture) emptyArray() else arrayOf(event.page)

            is MenuInteractEvent -> if (isFuture) emptyArray() else arrayOf(event.page)

            is PageTurnEvent -> arrayOf(if (isFuture) event.to else event.from)

            else -> emptyArray()
        }

    override fun toString(event: Event?, debug: Boolean) =
        if (isFuture) "the future event-page" else "the event-page"

    override fun isSingle() = true

    override fun getReturnType() = Number::class.java
}
