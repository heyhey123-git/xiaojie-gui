package io.github.heyhey123.xiaojiegui.skript.elements.menu.effects

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.event.MenuEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import io.github.heyhey123.xiaojiegui.skript.utils.ComponentHelper
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Update Page Title")
@Description(
    "Update the title of a specific page in a menu.",
    "The title is quoted text, or a value that already is a text component.",
    "You can optionally refresh the menu so that the players looking at that page see the new title",
    "immediately; players on other pages are left alone."
)
@Examples(
    // `in` takes the menu expression itself (the pattern has no `menu` keyword), and `menu {_menu}`
    // would be read as a lookup by id, which finds nothing.
    "update title of page 1 in {_menu} to \"New Page Title\" and refresh"
)
@Since("1.0.0")
class EffUpdatePageTitle : Effect() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.effect(
                addon,
                EffUpdatePageTitle::class.java,
                // One pattern per title form instead of a single group with two alternatives: when the
                // alternatives of a group have different types, Skript hands the literal over unparsed
                // (see EffSecCreateMenu) and the group's two slots moved every later slot. Both patterns
                // put the title at index 2, so the page and menu slots stay readable.
                "update title [of page %-number%] [in %-menu%] to string:%-string% [refresh:(and refresh)]",
                "update title [of page %-number%] [in %-menu%] to %-object% [refresh:(and refresh)]"
            )
        }
    }

    private var pageExpr: Expression<Number>? = null

    private var menuExpr: Expression<Menu>? = null

    private var titleExpr: Expression<Any>? = null

    private var refreshFlag: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        pageExpr = expressions?.getOrNull(0) as Expression<Number>?
        menuExpr = expressions?.getOrNull(1) as Expression<Menu>?
        if (menuExpr == null && !parser.isCurrentEvent(MenuEvent::class.java, ProvideMenuEvent::class.java)) {
            Skript.error("Menu expression is required if the current event is not a menu-related event.")
            return false
        }
        titleExpr = expressions?.getOrNull(2) as Expression<Any>?
        // Only the refresh tag is left in this pattern; the title form is the pattern that matched.
        refreshFlag = parseResult!!.hasTag("refresh")

        return true
    }

    override fun execute(event: Event?) {
        val menu = menuExpr?.getSingle(event) ?: when (event) {
            is MenuEvent -> event.menu
            is ProvideMenuEvent -> event.menu
            else -> {
                this.error("Menu cannot be null.")
                return
            }
        }

        val page = pageExpr?.getSingle(event)?.toInt() ?: menu.properties.defaultPage

        if (page !in 1..menu.size) {
            this.error("Page number $page is out of bounds for the menu.")
            return
        }

        val title = ComponentHelper.resolveTitleComponentOrNull(titleExpr, event, this)
        if (title == null) {
            this.error(
                "Title cannot be null."
            )
            return
        }
        menu.pages[page].title = title

        // Only the viewers looking at that page are retitled. A viewer on another page has a window
        // showing that other page, and its title did not change; giving them this one would put the
        // wrong title on their window until they turn the page.
        menu.sessionsOn(page).forEach { it.title(title, refreshFlag) }
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val sb = StringBuilder("update title of page ")
        pageExpr?.let {
            sb.append(it.toString(event, debug))
        } ?: sb.append("default page")

        sb.append(" in ").append(menuExpr?.toString(event, debug) ?: "event menu")

        if (titleExpr != null) {
            sb.append(" to ").append(titleExpr!!.toString(event, debug))
        } else {
            sb.append(" to <null title>")
        }

        if (refreshFlag) sb.append(" and refresh")
        return sb.toString()
    }
}
