package io.github.heyhey123.xiaojiegui.skript.elements.menu.event

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import io.github.heyhey123.xiaojiegui.gui.event.MenuOpenEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Menu Open")
@Description(
    "Event when a menu is opened.",
    "You can get the player who opened the menu, the menu, the session and the page number."
)
@Examples(
    "on menu open:",
    "    send \"You opened a menu on page %the page%.\" to player"
)
@Since("1.0.0")
class EvtMenuOpen : SkriptEvent() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.event(
                addon,
                EvtMenuOpen::class.java,
                "Menu Open",
                MenuOpenEvent::class.java,
                "menu open"
            )

            SkriptSyntax.eventValue(addon, MenuOpenEvent::class.java, Menu::class.java, MenuOpenEvent::menu)
            SkriptSyntax.eventValue(addon, MenuOpenEvent::class.java, MenuSession::class.java, MenuOpenEvent::session)
            SkriptSyntax.eventValue(addon, MenuOpenEvent::class.java, Player::class.java, MenuOpenEvent::viewer)
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
