package io.github.heyhey123.xiaojiegui.skript.elements.menu.event

import ch.njol.skript.Skript
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.registrations.EventValues
import io.github.heyhey123.xiaojiegui.gui.event.MenuCloseEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.bukkit.entity.Player
import org.bukkit.event.Event


@Name("Menu Close")
@Description(
    "Event when a menu is closed.",
    "You can get the player who closed the menu, the menu and the session."
)
@Since("1.0-SNAPSHOT")
class EvtMenuClose : SkriptEvent() {

    companion object {
        init {
            Skript.registerEvent(
                "Menu Close",
                EvtMenuClose::class.java,
                MenuCloseEvent::class.java,
                "menu close"
            )

            EventValues.registerEventValue(
                MenuCloseEvent::class.java,
                MenuSession::class.java,
                MenuCloseEvent::session
            )

            EventValues.registerEventValue(
                MenuCloseEvent::class.java,
                Player::class.java,
                MenuCloseEvent::viewer
            )

            EventValues.registerEventValue(
                MenuCloseEvent::class.java,
                Menu::class.java,
                MenuCloseEvent::menu
            )

        }
    }

    override fun init(
        args: Array<out Literal<*>?>?,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult?
    ) = true

    override fun check(event: Event?) = true

    override fun toString(event: Event?, debug: Boolean) = "menu close event"

}
