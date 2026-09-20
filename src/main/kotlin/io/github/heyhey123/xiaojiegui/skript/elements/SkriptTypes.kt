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
                // `window` is the word a script should reach for, and it is the one the guide and the
                // examples use: it is what a player sees after opening a menu, which Bukkit calls an
                // InventoryView. `session` is this addon's own word for the same thing and still parses,
                // so a script written with either one reads as itself.
                .user("menu ?sessions?", "menu ?windows?", "sessions?", "windows?")
                .name("Menu Window")
                .description(
                    "One player's view of a menu: which page they are on, what icons they have, what their",
                    "title says. A menu is the definition everybody shares -- its pages and layouts -- and a",
                    "window is a single player's use of it; changing a window changes nothing for anyone else.",
                    "Most scripts never name one: clicking, dragging, turning a page and opening a menu all",
                    "work without it, and inside a menu event `the window` is the one the event is about.",
                    "Naming it is for changing what one player sees -- their icons, their title, their list."
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
