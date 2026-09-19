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

@Name("Insert Page")
@Description(
    "Insert a new page into a menu.",
    "You can optionally specify the page index, layout, player inventory layout, and title for the new page.",
    "The title is quoted text, or a value that already is a text component.",
    "If the page index is not provided, the new page will be added at the end of the menu."
)
@Examples(
    // The menu has to be named outside a menu event, and the layout is one string per row: a single
    // comma-joined string is one row to the menu, not three.
    "insert page 1 to {_menu} with layout \"xxxxxxxxx\", \"xooooooxx\", \"xxxxxxxox\" " +
        "with player inventory layout \"ooooooooo\", \"oooooooox\", \"xxxxxxxxx\" with title \"New Page\""
)
@Since("1.0.0")
class EffInsertPage : Effect() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.effect(
                addon,
                EffInsertPage::class.java,
                // One pattern per title form instead of a single group with two alternatives: when the
                // alternatives of a group have different types, Skript hands the literal over unparsed
                // (see EffSecCreateMenu) and the group's two slots moved every later slot. Both patterns
                // keep the title at index 4; the layout and player inventory slots stay where they were.
                "insert page [%-numbers%] " +
                    "[to %-menu%] " +
                    "[with layout %-strings%] " +
                    "[with player inv[entory] layout %-strings%] " +
                    "[with [new] title string:%-string%]",
                "insert page [%-numbers%] " +
                    "[to %-menu%] " +
                    "[with layout %-strings%] " +
                    "[with player inv[entory] layout %-strings%] " +
                    "[with [new] title %-object%]"
            )
        }
    }

    private var pagesIndexesExpr: Expression<Number>? = null

    private var menuExpr: Expression<Menu>? = null

    private var layoutExpr: Expression<String>? = null

    private var playerInvLayoutExpr: Expression<String>? = null

    private var titleExpr: Expression<Any>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        pagesIndexesExpr = expressions?.get(0) as Expression<Number>?
        menuExpr = expressions?.get(1) as Expression<Menu>?
        if (menuExpr == null && !parser.isCurrentEvent(MenuEvent::class.java, ProvideMenuEvent::class.java)) {
            Skript.error("You must specify a menu to insert page to when not in a menu-related event.")
            return false
        }

        layoutExpr = expressions?.get(2) as Expression<String>?
        playerInvLayoutExpr = expressions?.get(3) as Expression<String>?
        titleExpr = expressions?.getOrNull(4) as Expression<Any>?

        return true
    }

    override fun execute(event: Event?) {
        val menu = menuExpr?.getSingle(event) ?: when (event) {
            is MenuEvent -> event.menu
            is ProvideMenuEvent -> event.menu
            else -> null
        }
        if (menu == null) {
            Skript.error("You must specify a menu to insert page to when not in a menu event.")
            return
        }
        val pagesIndexes = pagesIndexesExpr?.getArray(event)?.map { it.toInt() }.orEmpty()
        val layout = layoutExpr?.getArray(event)?.toList()
        val playerInvLayout = playerInvLayoutExpr?.getArray(event)?.toList()
        val title = ComponentHelper.resolveTitleComponentOrNull(titleExpr, event)

        if (pagesIndexes.isEmpty()) {
            // As the description promises: without a page index the new page is appended.
            menu.insertPage(null, layout, title, playerInvLayout)
            return
        }

        for (pageIndex in pagesIndexes) {
            menu.insertPage(pageIndex, layout, title, playerInvLayout)
        }
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val sb = StringBuilder("insert page")
        pagesIndexesExpr?.let {
            sb.append(' ').append(it.toString(event, debug))
        }

        // [to %-menu%], labelled as the event menu when it was not given.
        sb.append(" to ").append(menuExpr?.toString(event, debug) ?: "event menu")

        // [with layout %-strings%]
        layoutExpr?.let {
            sb.append(" with layout ").append(it.toString(event, debug))
        }

        // [with player inv[entory] layout %-strings%]
        playerInvLayoutExpr?.let {
            sb.append(" with player inv layout ").append(it.toString(event, debug))
        }

        // [with [new] title string:%-string%] or [with [new] title %-object%]
        titleExpr?.let {
            sb.append(" with new title ").append(it.toString(event, debug))
        }

        return sb.toString()
    }
}
