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
import io.github.heyhey123.xiaojiegui.skript.utils.ComponentHelper
import io.github.heyhey123.xiaojiegui.skript.utils.TitleType
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import org.bukkit.event.Event

@Name("Insert Page")
@Description(
    "Insert a new page into a menu.",
    "You can optionally specify the page index, layout, player inventory layout, and title for the new page.",
    "If the page index is not provided, the new page will be added at the end of the menu."
)
@Examples(
    "insert page 1 to menu with layout \"xxxxxxxxx, xooooooxx, xxxxxxxox\" with player inventory layout \"ooooooooo, oooooooox, xxxxxxxxx\" with title string:\"New Page\""
)
@Since("1.0-SNAPSHOT")
class EffInsertPage : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffInsertPage::class.java,
                "insert page [%-number%] " +
                        "[to %-menu%] " +
                        "[with layout %-strings%] " +
                        "[with player inv[entory] layout %-strings%] " +
                        "[with [new] title (string:%-string%|component:%-textcomponent%)]"
            )
        }
    }

    private var pageIndexExpr: Expression<Number>? = null

    private var menuExpr: Expression<Menu>? = null

    private var layoutExpr: Expression<String>? = null

    private var playerInvLayoutExpr: Expression<String>? = null

    private var titleStrExpr: Expression<String>? = null

    private var titleComponentExpr: Expression<Any>? = null

    private var titleType: TitleType? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        pageIndexExpr = expressions?.get(0) as Expression<Number>?
        menuExpr = expressions?.get(1) as Expression<Menu>?
        if (menuExpr == null && parser.isCurrentEvent(MenuEvent::class.java, ProvideMenuEvent::class.java)) {
            Skript.error("You must specify a menu to insert page to when not in a menu-related event.")
            return false
        }

        layoutExpr = expressions?.get(2) as Expression<String>?
        playerInvLayoutExpr = expressions?.get(3) as Expression<String>?
        if (parseResult!!.tags.isNotEmpty()) {
            titleType = TitleType.fromStringTag(parseResult.tags[0])
            titleStrExpr = expressions?.get(4) as Expression<String>?
            titleComponentExpr = expressions?.get(5) as Expression<Any>?
        }
        return true
    }

    override fun execute(event: Event?) {
        val menu = menuExpr!!.getSingle(event) ?: when (event) {
            is MenuEvent -> event.menu
            is ProvideMenuEvent -> event.menu
            else -> null
        }
        if (menu == null) {
            Skript.error("You must specify a menu to insert page to when not in a menu event.")
            return
        }
        val pageIndex = pageIndexExpr?.getSingle(event)?.toInt()
        val layout = layoutExpr?.getArray(event)?.toList()
        val playerInvLayout = playerInvLayoutExpr?.getArray(event)?.toList()
        val title = titleType?.let {
            ComponentHelper.resolveTitleComponentOrNull(
                titleStrExpr,
                titleComponentExpr,
                event,
                it
            )
        }

        menu.insertPage(pageIndex, layout, title, playerInvLayout)
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val sb = StringBuilder("insert page")
        pageIndexExpr?.let {
            sb.append(' ').append(it.toString(event, debug))
        }

        // [to %-menu%]，若未提供，则标记为 event menu
        sb.append(" to ").append(menuExpr?.toString(event, debug) ?: "event menu")

        // [with layout %-strings%]
        layoutExpr?.let {
            sb.append(" with layout ").append(it.toString(event, debug))
        }

        // [with player inv[entory] layout %-strings%]
        playerInvLayoutExpr?.let {
            sb.append(" with player inv layout ").append(it.toString(event, debug))
        }

        // [with [new] title (string:%-string%|component:%-textcomponent%)]
        when {
            titleStrExpr != null -> sb.append(" with new title string: ")
                .append(titleStrExpr!!.toString(event, debug))

            titleComponentExpr != null -> sb.append(" with new title component: ")
                .append(titleComponentExpr!!.toString(event, debug))
        }

        return sb.toString()
    }
}
