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
import io.github.heyhey123.xiaojiegui.skript.ComponentHelper
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import org.bukkit.event.Event


@Name("Update Page Title")
@Description(
    "Update the title of a specific page in a menu.",
    "You can optionally refresh the menu for all viewers to see the updated title immediately."
)
@Examples(
    "update title of page 2 in menu player's current menu to \"New Title\" and refresh"
)
@Since("1.0-SNAPSHOT")
class EffUpdatePageTitle : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffUpdatePageTitle::class.java,
                "update title of page %number% [in %-menu%] to %object% [refresh:(and refresh)]"
            )
        }
    }

    private lateinit var page: Expression<Number>

    private var menu: Expression<Menu>? = null

    private lateinit var title: Expression<Any>

    private var refresh: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        page = expressions?.get(0) as Expression<Number>
        menu = expressions[1] as Expression<Menu>?
        if (menu == null && !parser.isCurrentEvent(MenuEvent::class.java, ProvideMenuEvent::class.java)) {
            Skript.error("Menu expression is required if the current event is not a menu-related event.")
            return false
        }
        title = expressions[2] as Expression<Any>
        refresh = parseResult?.hasTag("refresh") ?: false

        return true
    }

    override fun execute(event: Event?) {
        val menu = menu?.getSingle(event) ?: when (event) {
            is MenuEvent -> event.menu
            is ProvideMenuEvent -> event.menu
            else -> {
                Skript.error("Menu cannot be null.")
                return
            }
        }

        val page = page.getAll(event)
        val title = title.getSingle(event)
        if (title == null) {
            Skript.error(
                "Title cannot be null."
            )
            return
        }

        val component = ComponentHelper.extractComponent(title) ?: return

        page.forEach { singlePage ->
            if (singlePage !in 0..<menu.size) return@forEach
            menu.pages.forEach { it.title = component }
        }

        menu.viewers.forEach { viewer ->
            val session = MenuSession.querySession(viewer) ?: return@forEach
            session.title(component, refresh)
        }

    }

    override fun toString(event: Event?, debug: Boolean) =
        "update title of page ${page.toString(event, debug)} in ${
            menu?.toString(
                event,
                debug
            )
        } to ${title.toString(event, debug)}${if (refresh) " and refresh" else ""}"
}
