package io.github.heyhey123.xiaojiegui.skript.elements.menu

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
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
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
                "(override|set) slot %number% " +
                        "in page [(number|index)] %number% " +
                        "of [(menu|gui)] %menu% to %itemstack%" +
                        "[(and|with) (refresh|update)]" +
                        "[and when (clicked|interacted|pressed)]"
            )
        }
    }

    private var trigger: TriggerItem? = null

    private lateinit var slot: Expression<Number>

    private lateinit var page: Expression<Number>

    private lateinit var menu: Expression<Menu>

    private lateinit var item: Expression<ItemStack>

    private var refresh: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?,
        sectionNode: SectionNode?,
        triggerItems: List<TriggerItem?>?
    ): Boolean {
        slot = expressions!![0] as Expression<Number>
        page = expressions[1] as Expression<Number>
        menu = expressions[2] as Expression<Menu>
        item = expressions[3] as Expression<ItemStack>

        if (parseResult!!.hasTag("refresh")) {
            refresh = true
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
        val menu = menu.getSingle(event)
        val slot = slot.getAll(event)
        val page = page.getAll(event)
        val item = item.getSingle(event)

        if (menu == null) {
            Skript.error("Menu cannot be null.")
            return walk(event, false)
        }

        if (slot.isEmpty()) {
            Skript.error("Slot cannot be null.")
            return walk(event, false)
        }

        if (page.isEmpty()) {
            Skript.error("Page cannot be empty.")
            return walk(event, false)
        }

        if (trigger == null) {
            for (singlePage in page) {
                for (singleSlot in slot) {
                    menu.overrideSlot(
                        singleSlot.toInt(),
                        singlePage.toInt(),
                        item,
                        refresh
                    )
                }

            }
            return walk(event, false)
        }

        for (singlePage in page) {
            for (singleSlot in slot) {
                menu.overrideSlot(
                    singleSlot.toInt(),
                    singlePage.toInt(),
                    item,
                    refresh
                ) { menuEvent ->
                    try {
                        Variables.withLocalVariables(event, menuEvent) {
                            TriggerItem.walk(trigger, menuEvent)
                        }
                    } catch (e: Throwable) {
                        val id = menu.id ?: "<unnamed>"

                        Skript.exception(
                            e,
                            Thread.currentThread(),
                            "Error occurred in a slot callback for menu $id.This callback was added when overriding slot $slot in page $singlePage to item $item."
                        )
                    }
                }
            }
        }

        return walk(event, false)
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val slotStr = slot.toString(event, debug)
        val pageStr = page.toString(event, debug)
        val menuStr = menu.toString(event, debug)
        val itemStr = item.toString(event, debug)
        val base = "override slot $slotStr in page $pageStr of menu $menuStr to $itemStr"

        return if (hasSection()) "$base and when clicked do ..." else base
    }
}
