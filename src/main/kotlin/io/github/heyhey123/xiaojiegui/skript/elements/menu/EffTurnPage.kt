package io.github.heyhey123.xiaojiegui.skript.elements.menu

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.ComponentHelper
import org.bukkit.entity.Player
import org.bukkit.event.Event


@Name("Turn Page")
@Description(
    "Turn the page of the currently open menu for a player.",
    "You can optionally specify a new title for the menu."
)
@Examples(
    "turn to page 2 for player with new title \"New Page Title\""
)
@Since("1.0-SNAPSHOT")
class EffTurnPage : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                EffTurnPage::class.java,
                "turn to page %number% for %player% [with (new) title %-string%]"
            )
        }
    }

    private lateinit var page: Expression<Number>

    private lateinit var player: Expression<Player>

    private var newTitle: Expression<Any?>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?
    ): Boolean {
        page = expressions?.get(0) as Expression<Number>
        player = expressions[1] as Expression<Player>
        newTitle = expressions[2] as Expression<Any?>?
        return true
    }

    override fun execute(event: Event?) {
        val page = page.getSingle(event)?.toInt()
        if (page == null) {
            Skript.error("Page number cannot be null.")
            return
        }
        val player = player.getSingle(event)
        if (player == null) {
            Skript.error("Player cannot be null.")
            return
        }
        val session = MenuSession.querySession(player)
        val menu = session?.menu
        if (session == null || menu == null) {
            Skript.error("Player $player does not have an open menu session.")
            return
        }
        val title = newTitle?.let { ComponentHelper.extractComponent(it, event) }
        menu.turnPage(player, page, title)
    }

    override fun toString(event: Event?, debug: Boolean) =
        "turn page to page $page for $player with new title $newTitle"

}
