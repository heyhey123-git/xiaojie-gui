package io.github.heyhey123.xiaojiegui.skript.elements

import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.classes.EnumClassInfo
import ch.njol.skript.expressions.base.EventValueExpression
import ch.njol.skript.registrations.Classes
import io.github.heyhey123.xiaojiegui.gui.interact.BukkitClickType
import io.github.heyhey123.xiaojiegui.gui.interact.ClickMode
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.github.heyhey123.xiaojiegui.skript.ComponentHelper
import org.bukkit.inventory.ItemStack
import org.skriptlang.skript.lang.converter.Converters

object SkriptTypes {
    init {
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
                .user("menu sessions?", "session")
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

        Classes.registerClass(
            EnumClassInfo(
                Receptacle.Mode::class.java,
                "receptaclemode",
                "receptacle mode"
            )
                .user("receptacle modes?", "modes?")
                .name("Receptacle Mode")
                .description(
                    "Represents the underlying implementation mode of a receptacle, which defines how player interactions are handled.",
                    "There are two modes: 'static' and 'phantom'.",
                    "'static' mode uses a standard server-side inventory." +
                            " The implementation relies on Bukkit's built-in inventory system," +
                            " meaning that items are physically moved in and out of the inventory when players interact with it." +
                            " This mode is straightforward and works well for simple use cases " +
                            "where you want players to be able to pick up and move items around.",
                    "'phantom' mode uses a virtual inventory simulated via packets." +
                            " In this mode, items are not actually moved in the inventory; " +
                            "instead, the server sends packets to the client to simulate item movements. " +
                            "This allows for more complex interactions and behaviors, " +
                            "such as preventing players from taking items out of the inventory " +
                            "while still allowing them to interact with the items in other ways.",
                )
                .since("1.0-SNAPSHOT")
        )

        Classes.registerClass(
            EnumClassInfo(
                ClickType::class.java,
                "menuclicktype",
                "menu click type"
            )
                .user("menu click types?")
                .name("Menu Click Type")
                .documentationId("Menu Click Type")
                .description("Represents different types of clicks in a GUI menu.")
                .since("1.0-SNAPSHOT")
        )

        Converters.registerConverter(
            ClickType::class.java,
            BukkitClickType::class.java,
            ClickType::bukkitClickType
        )

        Classes.registerClass(
            EnumClassInfo(
                ClickMode::class.java,
                "menuclickmode",
                "menu click mode"
            )
                .user("menu click modes?", "click modes?")
                .name("Menu Click Mode")
                .description("Represents different click modes in a GUI menu.")
                .since("1.0-SNAPSHOT")
        )

        Classes.registerClass(
            ClassInfo(Page::class.java, "page")
                .user("pages?")
                .name("Page")
                .description("Represents a page in a GUI menu.")
                .since("1.0-SNAPSHOT")
        )

        Converters.registerConverter(
            Page::class.java,
            Int::class.java,
            Page::convert
        )

        Classes.registerClass(
            ClassInfo(Slot::class.java, "menuslot")
                .user("menu slots?", "slots?")
                .name("Menu Slot")
                .description(
                    "Represents a slot in a GUI menu.",
                    "Tips: Skript doesn't like \"Slot\"."
                )
                .documentationId("Menu　Slot")
                .since("1.0-SNAPSHOT")
        )

        Converters.registerConverter(
            Slot::class.java,
            Int::class.java,
            Slot::convert
        )

        Classes.registerClass(
            ClassInfo(Icon::class.java, "icon")
                .user("icons?")
                .name("Icon")
                .description("Represents an icon (item) in a GUI menu.")
                .since("1.0-SNAPSHOT")
        )

        Converters.registerConverter(
            Icon::class.java,
            ItemStack::class.java,
            Icon::convert
        )

        Classes.registerClass(
            ClassInfo(Title::class.java, "menutitle")
                .user("menu titles?", "titles?")
                .name("Menu Title")
                .description("Represents a title in a GUI menu.")
                .since("1.0-SNAPSHOT")
        )

        @Suppress("UNCHECKED_CAST")
        Converters.registerConverter(
            Title::class.java,
            ComponentHelper.componentWrapperType::class.java as Class<Any>,
            Title::convert
        )

    }
}

/**
 * A generic wrapper class for syntax elements used in Skript.
 *
 * @param T the type of the wrapped value
 */
abstract class SyntaxWrapper<T> {

    /**
     * The wrapped value.
     */
    abstract val value: T

    /**
     * Converts the wrapped value to its underlying type.
     *
     * @return the underlying value
     */
    fun convert(): T? = value

    override fun toString() = value.toString()
}

/**
 * Represents a page in a GUI menu.
 * Used for add syntactic sugar in Skript.
 *
 * @property value the page number
 */
class Page(override val value: Int) : SyntaxWrapper<Int>()

/**
 * Represents a slot in a GUI menu.
 * Used for add syntactic sugar in Skript.
 *
 * @property value the slot number
 */
class Slot(override val value: Int) : SyntaxWrapper<Int>()

/**
 * Represents an icon (item) in a GUI menu.
 * Used for add syntactic sugar in Skript.
 *
 * @property value the ItemStack representing the icon
 */
class Icon(override val value: ItemStack?) : SyntaxWrapper<ItemStack?>()

/**
 * Represents a title in a GUI menu.
 *
 * @property value the title. Can be `String` or `ComponentWrapper` in SkBee.
 */
class Title(override val value: Any) : SyntaxWrapper<Any>()
