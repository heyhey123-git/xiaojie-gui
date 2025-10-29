package io.github.heyhey123.xiaojiegui.skript.elements.menu.sections

import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.EffectSection
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.lang.util.SectionUtils
import ch.njol.skript.variables.Variables
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.event.MenuEvent
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

@Name("Override Slot")
@Description(
    "Override a specific slot in a specific page of a menu with a new item.",
    "You can optionally provide a section to handle click events on the overridden slot."
)
@Examples(
    "override slot 0 in page 1 of menu {_menu} to stone named \"Click me!\" and refresh and when clicked:",
    "send \"You clicked the overridden slot!\" to player"
)
@Since("1.0-SNAPSHOT")
class EffSecOverrideSlot : EffectSection() {

    companion object {
        init {
            Skript.registerSection(
                EffSecOverrideSlot::class.java,
                "(override|set) slot %numbers% " +
                        "in page [(number|index)] %numbers% " +
                        "[of [(menu|gui)] %-menu%] to %itemstack%" +
                        "[refresh:((and|with) (refresh|update))]" +
                        "[when:(and when (clicked|interacted|pressed))]"
            )
        }
    }

    private var trigger: TriggerItem? = null

    private lateinit var slotsExpr: Expression<Number>

    private lateinit var pagesExpr: Expression<Number>

    private var menuExpr: Expression<Menu>? = null

    private lateinit var itemExpr: Expression<ItemStack>

    private var refreshFlag: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?,
        sectionNode: SectionNode?,
        triggerItems: List<TriggerItem?>?
    ): Boolean {
        slotsExpr = expressions!![0] as Expression<Number>
        pagesExpr = expressions[1] as Expression<Number>
        menuExpr = expressions[2] as Expression<Menu>?
        itemExpr = expressions[3] as Expression<ItemStack>

        if (parseResult!!.hasTag("refresh")) {
            refreshFlag = true
        }

        if (parseResult.hasTag("when") && !hasSection()) {
            Skript.error("You must provide a section to handle the click event when using 'and when clicked'.")
            return false
        }

        if (hasSection()) {
            trigger = SectionUtils.loadLinkedCode(
                "override slot"
            ) { beforeLoading: Runnable?, afterLoading: Runnable? ->
                loadCode(
                    sectionNode,
                    "override slot",
                    beforeLoading,
                    afterLoading,
                    MenuInteractEvent::class.java
                )
            }
        }

        return true
    }

    override fun walk(event: Event?): TriggerItem? {
        val menu = menuExpr?.getSingle(event) ?: when (event) {
            is MenuEvent -> event.menu
            is ProvideMenuEvent -> event.menu
            else -> null
        }
        if (menu == null) {
            Skript.error("Menu cannot be null.")
            return walk(event, false)
        }

        val slots = slotsExpr.getAll(event)
        if (slots.isEmpty()) {
            Skript.error("Slot cannot be null.")
            return walk(event, false)
        }

        val pages = pagesExpr.getAll(event)
        if (pages.isEmpty()) {
            Skript.error("Page cannot be empty.")
            return walk(event, false)
        }

        if (pages.any { it.toInt() !in 0..<menu.pages.size }) {
            Skript.error("One or more page numbers are out of bounds for the menu.")
            return walk(event, false)
        }

        val item = itemExpr.getSingle(event)

        if (trigger == null) {
            for (singlePage in pages) {
                for (singleSlot in slots) {
                    menu.overrideSlot(
                        singleSlot.toInt(),
                        singlePage.toInt(),
                        item,
                        refreshFlag
                    )
                }

            }
            return walk(event, false)
        }

        for (singlePage in pages) {
            for (singleSlot in slots) {
                menu.overrideSlot(
                    singleSlot.toInt(),
                    singlePage.toInt(),
                    item,
                    refreshFlag
                ) { menuEvent ->
                    try {
                        Variables.withLocalVariables(event, menuEvent) {
                            walk(trigger, menuEvent)
                        }
                    } catch (e: Throwable) {
                        val id = menu.id ?: "<unnamed>"

                        Skript.exception(
                            e,
                            Thread.currentThread(),
                            "Error occurred in a slot callback for menu $id.This callback was added when overriding slot $slots in page $singlePage to item $item."
                        )
                    }
                }
            }
        }

        return walk(event, false)
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val slotStr = slotsExpr.toString(event, debug)
        val pageStr = pagesExpr.toString(event, debug)
        val menuStr = menuExpr?.toString(event, debug)
        val itemStr = itemExpr.toString(event, debug)
        val base = "override slot $slotStr in page $pageStr of menu $menuStr to $itemStr"

        return if (hasSection()) "$base and when clicked do ..." else base
    }
}
