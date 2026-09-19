package io.github.heyhey123.xiaojiegui.skript.elements.menu.event

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Menu Interact")
@Description(
    "Event when a player interacts with a menu.",
    "You can get the menu, the session, the page, the slot, the icon and the click type.",
    "The click type is Skript's own click type, so it is compared with the words `on inventory click`",
    "uses: `left mouse button`, `right mouse button`, `left mouse button with shift`, `number key`, ..."
)
@Examples(
    "on menu interact:",
    "    send \"You clicked slot %the clicked slot% on page %the page%.\" to player",
    "    if the event-clicktype is left mouse button:",
    "        send \"Left click!\" to player"
)
@Since("1.0.0")
class EvtMenuInteract : SkriptEvent() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.event(
                addon,
                EvtMenuInteract::class.java,
                "Menu Interact",
                MenuInteractEvent::class.java,
                "menu interact"
            )

            // The click type is published as the addon's own, richer type; a script reads it as
            // Skript's own click type, which Skript reaches through the converter registered in
            // `SkriptTypes`.
            SkriptSyntax.eventValue(addon, MenuInteractEvent::class.java, Menu::class.java, MenuInteractEvent::menu)
            SkriptSyntax.eventValue(addon, MenuInteractEvent::class.java, MenuSession::class.java, MenuInteractEvent::session)
            SkriptSyntax.eventValue(addon, MenuInteractEvent::class.java, Player::class.java, MenuInteractEvent::viewer)
            SkriptSyntax.eventValue(addon, MenuInteractEvent::class.java, ClickType::class.java, MenuInteractEvent::clickType)
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
