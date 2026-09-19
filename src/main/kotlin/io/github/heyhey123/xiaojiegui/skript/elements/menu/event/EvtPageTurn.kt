package io.github.heyhey123.xiaojiegui.skript.elements.menu.event

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import io.github.heyhey123.xiaojiegui.gui.event.PageTurnEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.skriptlang.skript.addon.SkriptAddon

@Name("Page Turn")
@Description(
    "Event when a player turns a page in a menu.",
    "You can get the player who turned the page, the menu session, the from and to page numbers, and the title of the new page."
)
@Examples(
    "on page turn:",
    "    send \"You turned from page %the page% to page %the future page%.\" to player"
)
@Since("1.0.0")
class EvtPageTurn : SkriptEvent() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.event(
                addon,
                EvtPageTurn::class.java,
                "Page Turn",
                PageTurnEvent::class.java,
                "page turn"
            )

            SkriptSyntax.eventValue(addon, PageTurnEvent::class.java, MenuSession::class.java, PageTurnEvent::session)
            SkriptSyntax.eventValue(addon, PageTurnEvent::class.java, Player::class.java, PageTurnEvent::viewer)
            SkriptSyntax.eventValue(addon, PageTurnEvent::class.java, Menu::class.java, PageTurnEvent::menu)
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
