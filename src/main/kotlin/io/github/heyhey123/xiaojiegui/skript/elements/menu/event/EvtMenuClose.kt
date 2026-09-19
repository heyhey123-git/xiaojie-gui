package io.github.heyhey123.xiaojiegui.skript.elements.menu.event

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import io.github.heyhey123.xiaojiegui.gui.event.MenuCloseEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Menu Close")
@Description(
    "Event when a menu is closed.",
    "You can get the player who closed the menu, the menu and the session."
)
@Examples(
    "on menu close:",
    "    send \"You closed the menu.\" to player"
)
@Since("1.0.0")
class EvtMenuClose : SkriptEvent() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.event(
                addon,
                EvtMenuClose::class.java,
                "Menu Close",
                MenuCloseEvent::class.java,
                "menu close"
            )

            SkriptSyntax.eventValue(addon, MenuCloseEvent::class.java, MenuSession::class.java, MenuCloseEvent::session)
            SkriptSyntax.eventValue(addon, MenuCloseEvent::class.java, Player::class.java, MenuCloseEvent::viewer)
            SkriptSyntax.eventValue(addon, MenuCloseEvent::class.java, Menu::class.java, MenuCloseEvent::menu)
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
