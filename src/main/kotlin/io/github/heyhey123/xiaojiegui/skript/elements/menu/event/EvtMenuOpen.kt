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
import io.github.heyhey123.xiaojiegui.gui.event.MenuOpenEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.elements.Page
import org.bukkit.entity.Player
import org.bukkit.event.Event


@Name("Open Menu")
@Description(
    "Event when a menu is opened.",
    "You can get the player who opened the menu, the menu, the session and the page number."
)
@Examples(
    "on menu open:",
    "send \"You opened menu %menu% on page %page%\" to player"
)
@Since("1.0-SNAPSHOT")
class EvtMenuOpen : SkriptEvent() {

    companion object {
        init {
            Skript.registerEvent(
                "Menu Open",
                EvtMenuOpen::class.java,
                MenuOpenEvent::class.java,
                "menu open"
            )

            EventValues.registerEventValue(
                MenuOpenEvent::class.java,
                Menu::class.java,
                MenuOpenEvent::menu
            )

            EventValues.registerEventValue(
                MenuOpenEvent::class.java,
                Page::class.java
            ) { event -> Page(event.page) }

            EventValues.registerEventValue(
                MenuOpenEvent::class.java,
                MenuSession::class.java,
                MenuOpenEvent::session
            )

            EventValues.registerEventValue(
                MenuOpenEvent::class.java,
                Player::class.java,
                MenuOpenEvent::viewer
            )
        }
    }

    override fun init(
        args: Array<out Literal<*>?>?,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult?
    ) = true

    override fun check(event: Event?) = true

    override fun toString(event: Event?, debug: Boolean) = "menu open event"
}
