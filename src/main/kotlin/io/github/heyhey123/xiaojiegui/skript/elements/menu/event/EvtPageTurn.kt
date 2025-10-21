package io.github.heyhey123.xiaojiegui.skript.elements.menu.event

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.registrations.EventValues
import io.github.heyhey123.xiaojiegui.gui.event.PageTurnEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.bukkit.entity.Player
import org.bukkit.event.Event
import kotlin.jvm.java


@Name("Page Turn")
@Description(
    "Event when a player turns a page in a menu.",
    "You can get the player who turned the page, the menu session, the from and to page numbers, and the title of the new page."
)
@Examples(
    "on page turn:",
    "send \"You turned from page %past number% to page %future number% in menu %session's menu id%\" to player"
)
@Since("1.0-SNAPSHOT")
class EvtPageTurn : SkriptEvent() {

    companion object {
        init {
            Skript.registerEvent(
                "Page Turn",
                EvtPageTurn::class.java,
                PageTurnEvent::class.java,
                "page turn"
            )

            EventValues.registerEventValue(
                PageTurnEvent::class.java,
                MenuSession::class.java,
                PageTurnEvent::session
            )

            EventValues.registerEventValue(
                PageTurnEvent::class.java,
                Player::class.java,
                PageTurnEvent::viewer
            )
            EventValues.registerEventValue(
                PageTurnEvent::class.java,
                Menu::class.java,
                PageTurnEvent::menu
            )
        }
    }

    override fun init(
        args: Array<out Literal<*>?>?,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult?
    ): Boolean = true

    override fun check(event: Event?) = true

    override fun toString(event: Event?, debug: Boolean) = "page turn event"

}
