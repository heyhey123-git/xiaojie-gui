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
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.elements.Icon
import io.github.heyhey123.xiaojiegui.skript.elements.Page
import io.github.heyhey123.xiaojiegui.skript.elements.Slot
import org.bukkit.entity.Player
import org.bukkit.event.Event


@Name("Menu Interact")
@Description(
    "Event when a player interacts with a menu.",
    "You can get the menu, the session, the page, the slot, the icon and the click type."
)
@Examples(
    "on menu interact:",
    "send \"You clicked on slot %slot of page %page% in menu %menu% with %click type% click.\" to player"
)
@Since("1.0-SNAPSHOT")
class EvtMenuInteract : SkriptEvent() {

    companion object {
        init {
            Skript.registerEvent(
                "Menu Interact",
                EvtMenuInteract::class.java,
                MenuInteractEvent::class.java,
                "menu interact"
            )

            EventValues.registerEventValue(
                MenuInteractEvent::class.java,
                Menu::class.java,
                MenuInteractEvent::menu
            )

            EventValues.registerEventValue(
                MenuInteractEvent::class.java,
                MenuSession::class.java,
                MenuInteractEvent::session
            )

            EventValues.registerEventValue(
                MenuInteractEvent::class.java,
                Player::class.java,
                MenuInteractEvent::viewer
            )

            EventValues.registerEventValue(
                MenuInteractEvent::class.java,
                Page::class.java
            ) { event -> Page(event.page) }

            EventValues.registerEventValue(
                MenuInteractEvent::class.java,
                Slot::class.java
            ) { event -> Slot(event.slot) }

            EventValues.registerEventValue(
                MenuInteractEvent::class.java,
                Icon::class.java
            ) { event -> Icon(event.icon) }

            EventValues.registerEventValue(
                MenuInteractEvent::class.java,
                ClickType::class.java,
                MenuInteractEvent::clickType
            )
        }
    }

    override fun init(
        args: Array<out Literal<*>?>?,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult?
    ) = true

    override fun check(event: Event?) = true

    override fun toString(event: Event?, debug: Boolean) = "menu interact event"
}
