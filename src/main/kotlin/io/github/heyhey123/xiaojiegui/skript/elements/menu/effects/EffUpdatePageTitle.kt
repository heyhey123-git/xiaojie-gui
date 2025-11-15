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
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import io.github.heyhey123.xiaojiegui.skript.utils.ComponentHelper
import io.github.heyhey123.xiaojiegui.skript.utils.TitleType
import org.bukkit.event.Event


@Name("Update Page Title")
@Description(
    "Update the title of a specific page in a menu.",
    "You can optionally refresh the menu for all viewers to see the updated title immediately."
)
@Examples(
    "update title of page 1 in menu {_menu} to \"New Page Title\" and refresh",
)
@Since("1.0-SNAPSHOT")
class EffUpdatePageTitle : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffUpdatePageTitle::class.java,
                "update title [of page %-number%] [in %-menu%] to (string:%-string%|component:%-textcomponent%) [refresh:(and refresh)]"
            )
        }
    }

    private var pageExpr: Expression<Number>? = null

    private var menuExpr: Expression<Menu>? = null

    private var titleStrExpr: Expression<String>? = null

    private var titleComponentExpr: Expression<Any>? = null

    private lateinit var titleType: TitleType

    private var refreshFlag: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        pageExpr = expressions?.get(0) as Expression<Number>
        menuExpr = expressions[1] as Expression<Menu>?
        if (menuExpr == null && !parser.isCurrentEvent(MenuEvent::class.java, ProvideMenuEvent::class.java)) {
            Skript.error("Menu expression is required if the current event is not a menu-related event.")
            return false
        }
        titleStrExpr = expressions[2] as Expression<String>?
        titleComponentExpr = expressions[3] as Expression<Any>?
        titleType = TitleType.fromStringTag(parseResult!!.tags[0])
        refreshFlag = parseResult.hasTag("refresh")

        return true
    }

    override fun execute(event: Event?) {
        val menu = menuExpr?.getSingle(event) ?: when (event) {
            is MenuEvent -> event.menu
            is ProvideMenuEvent -> event.menu
            else -> {
                Skript.error("Menu cannot be null.")
                return
            }
        }

        val page = pageExpr?.getSingle(event)?.toInt() ?: menu.properties.defaultPage

        if (page !in 0..<menu.size) {
            Skript.error("Page number $page is out of bounds for the menu.")
            return
        }

        val title = ComponentHelper.resolveTitleComponentOrNull(
            titleStrExpr,
            titleComponentExpr,
            event,
            titleType
        )
        if (title == null) {
            Skript.error(
                "Title cannot be null."
            )
            return
        }
        menu.pages[page].title = title

        menu.viewers.forEach { viewer ->
            val session = MenuSession.querySession(viewer) ?: return@forEach
            session.title(title, refreshFlag)
        }

    }

    override fun toString(event: Event?, debug: Boolean): String {
        val sb = StringBuilder("update title of page ")
        pageExpr?.let {
            sb.append(it.toString(event, debug))
        } ?: sb.append("default page")

        sb.append(" in ").append(menuExpr?.toString(event, debug) ?: "event menu")

        when {
            titleStrExpr != null -> sb.append(" to string: ").append(titleStrExpr!!.toString(event, debug))
            titleComponentExpr != null -> sb.append(" to component: ")
                .append(titleComponentExpr!!.toString(event, debug))

            else -> sb.append(" to <null title>")
        }

        if (refreshFlag) sb.append(" and refresh")
        return sb.toString()
    }
}
