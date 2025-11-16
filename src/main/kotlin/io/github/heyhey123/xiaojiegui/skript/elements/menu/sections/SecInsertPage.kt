package io.github.heyhey123.xiaojiegui.skript.elements.menu.sections

import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.event.MenuEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import io.github.heyhey123.xiaojiegui.skript.utils.ComponentHelper
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.Event
import org.skriptlang.skript.lang.entry.EntryValidator
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData


@Name("Build and Insert Page")
@Description(
    "Insert a new page into a menu.",
    "You can optionally specify the page index, layout, player inventory layout, and title for the new page.",
    "If the page index is not provided, the new page will be added at the end of the menu."
)
@Examples(
    "insert page 1 into menu:",
    "    layout: \"xxxxxxxxx, xooooooxx, xxxxxxxox\"",
    "    player inventory layout: \"ooooooooo, oooooooox, xxxxxxxxx\"",
    "    title: \"New Page\""
)
@Since("1.0-SNAPSHOT")
class SecInsertPage : Section() {

    companion object {
        init {
            Skript.registerSection(
                SecInsertPage::class.java,
                "insert page [%-number%] into menu [%-menu%]"
            )
        }
    }

    private var pageIndexExpr: Expression<Number>? = null

    private var menuExpr: Expression<Menu>? = null

    private var layoutExpr: Expression<String>? = null

    private var playerInvLayoutExpr: Expression<String>? = null

    private var titleExpr: Expression<Any>? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?,
        sectionNode: SectionNode?,
        triggerItems: List<TriggerItem?>?
    ): Boolean {
        pageIndexExpr = expressions?.get(0) as Expression<Number>?
        menuExpr = expressions?.get(1) as Expression<Menu>?
        if (
            menuExpr == null &&
            !parser.isCurrentEvent(ProvideMenuEvent::class.java, MenuEvent::class.java)
        ) {
            Skript.error("Menu expression is required unless in a menu event.")
            return false
        }

        return parseNode(sectionNode!!)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseNode(sectionNode: SectionNode): Boolean {
        val validator = EntryValidator.builder()
            .addEntryData(ExpressionEntryData("layout", null, true, String::class.java))
            .addEntryData(ExpressionEntryData("player inventory layout", null, true, String::class.java))
            .addEntryData(
                ExpressionEntryData(
                    "title",
                    null,
                    true,
                    *ComponentHelper.titleReturnTypes as Array<Class<Any>>
                )
            )
            .build()

        val container = validator.validate(sectionNode)

        if (container == null) {
            Skript.error("Invalid insert page section syntax. Please, check your syntax.")
            return false
        }

        layoutExpr = container.getOptional("layout", false) as Expression<String>?

        playerInvLayoutExpr = container.getOptional("player inventory layout", false) as Expression<String>?

        titleExpr = container.getOptional("title", false) as Expression<Any>?

        return true
    }

    override fun walk(event: Event?): TriggerItem? {
        val menu = menuExpr?.getSingle(event) ?: when (event) {
            is MenuEvent -> event.menu
            is ProvideMenuEvent -> event.menu
            else -> null
        }
        if (menu == null) {
            Skript.error("You must specify a menu to insert page to when not in a menu event.")
            return walk(event, false)
        }

        val pageIndex = pageIndexExpr?.getSingle(event)?.toInt()

        val layout = layoutExpr?.getArray(event)?.toList()
        val playerInvLayout = playerInvLayoutExpr?.getArray(event)?.toList()

        var title: Component? = null
        titleExpr?.let {
            val titleData = it.getSingle(event) ?: return@let

            if (
                ComponentHelper.hasSkBee &&
                it.canReturn(ComponentHelper.skbeeComponentWrapper)
            ) {
                title = ComponentHelper.extractComponent(titleData)
            } else {
                val convertedTitle = titleExpr!!.getConvertedExpression(String::class.java)?.getSingle(event)
                if (convertedTitle == null) {
                    Skript.error("The given menu title is not a textcomponent, and cannot be converted to string.")
                    return walk(event, false)
                }
                title = LegacyComponentSerializer.legacySection().deserialize(convertedTitle)
            }
        }

        menu.insertPage(
            pageIndex,
            layoutPattern = layout,
            title = title,
            playerInventoryPattern = playerInvLayout,
        )

        return walk(event, false)
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val sb = StringBuilder("insert page")
        pageIndexExpr?.let {
            sb.append(' ').append(it.toString(event, debug))
        }
        sb.append(" into menu ").append(menuExpr?.toString(event, debug) ?: "current menu")
        layoutExpr?.let {
            sb.append(" with layout ").append(it.toString(event, debug))
        }
        playerInvLayoutExpr?.let {
            sb.append(" with player inv layout ").append(it.toString(event, debug))
        }
        titleExpr?.let {
            sb.append(" with new title ").append(it.toString(event, debug))
        }
        return sb.toString()
    }
}
