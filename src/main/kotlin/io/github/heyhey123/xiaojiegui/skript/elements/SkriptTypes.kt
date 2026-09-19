package io.github.heyhey123.xiaojiegui.skript.elements

import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.expressions.base.EventValueExpression
import ch.njol.skript.registrations.Classes
import io.github.heyhey123.xiaojiegui.gui.interact.BukkitClickType
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.skriptlang.skript.lang.converter.Converters

object SkriptTypes {

    /**
     * Registers the classes and the one converter this addon adds to Skript.
     *
     * It is a function rather than an initializer because registration has to happen while the plugin
     * enables, at the point [io.github.heyhey123.xiaojiegui.skript.registerElements] says so, instead of
     * the first time something happens to touch this object.
     */
    fun register() {
        Classes.registerClass(
            ClassInfo(Menu::class.java, "menu")
                .user("menus?", "guis?")
                .name("Menu")
                .description("Represents a GUI menu.")
                .defaultExpression(EventValueExpression(Menu::class.java))
                .since("1.0-SNAPSHOT")
        )

        Classes.registerClass(
            ClassInfo(MenuSession::class.java, "menusession")
                .user("menu ?sessions?", "sessions?")
                .name("Menu Session")
                .description(
                    "Represents a specific interaction session a player has with a menu.",
                    "A new menu session is created for a player when they open a menu, and it is destroyed when they close it.",
                    "This is very useful as it allows multiple players to have the same menu open simultaneously. A menu session enables you to distinguish between different players and manage specific data or actions for each player within the menu.",
                    "For example, you can get the corresponding player or menu from a session."
                )
                .defaultExpression(EventValueExpression(MenuSession::class.java))
                .since("1.0-SNAPSHOT")
        )

        // `the event-clicktype` in `on menu interact` is Skript's own click type -- the one
        // `on inventory click` hands out, with its words (`left mouse button`, `Shift+LMB`, ...) and its
        // literals. It gets there through this converter: `EventValues.registerEventValue` publishes the
        // addon's richer click type, and Skript resolves an event value of another type through
        // registered converters.
        //
        // The click type is deliberately not registered as a type of its own. An addon's enum literals
        // are parsed from Skript language files, which this addon does not ship, so its only literal
        // would be the generated `left menu click type` and printing it would give `LEFT` -- a second,
        // worse name for something Skript already names well.
        Converters.registerConverter(
            ClickType::class.java,
            BukkitClickType::class.java,
            ClickType::bukkitClickType
        )
    }
}
