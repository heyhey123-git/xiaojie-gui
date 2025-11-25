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
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.event.MenuEvent
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import io.github.heyhey123.xiaojiegui.skript.utils.Button
import io.github.heyhey123.xiaojiegui.skript.utils.MenuCallbackUtils
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

@Name("Override Slot")
@Description(
    "Override a specific slot in a specific page of a menu with a new item.",
    "You can optionally provide a section to handle click events on the overridden slot.",
    "Tips: If you didn't provide a section, the slot will simply be overridden without changing previous click behavior."
)
@Examples(
    "override slot 10 in page 0 of menu with id \"main_menu\" to diamond named \"Clicked Item\" refresh and when clicked:",
    "    send \"You clicked the overridden slot!\" to player"
)
@Since("1.0-SNAPSHOT")
class EffSecOverrideSlot : EffectSection() {

    companion object {
        init {
            Skript.registerSection(
                EffSecOverrideSlot::class.java,
                "(override|set) slot %numbers% " +
                        "[in page %-numbers%] " +
                        "to (%-itemstack%|button %-string%) " +
                        "for menu %-menu% " +
                        "[refresh:((and|with) (refresh|update))] " +
                        "[when:(and when (clicked|interacted|pressed))]"
            )
        }
    }

    private var trigger: TriggerItem? = null

    private lateinit var slotsExpr: Expression<Number>

    private var menuExpr: Expression<Menu>? = null

    private var pagesExpr: Expression<Number>? = null

    private var itemExpr: Expression<ItemStack>? = null

    private var buttonIdExpr: Expression<String>? = null

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
        pagesExpr = expressions[1] as Expression<Number>?
        itemExpr = expressions[2] as Expression<ItemStack>?
        buttonIdExpr = expressions[3] as Expression<String>?
        menuExpr = expressions[4] as Expression<Menu>?

        if (parseResult!!.hasTag("refresh")) {
            refreshFlag = true
        }

        if (parseResult.hasTag("when") && !hasSection()) {
            Skript.error("You must provide a section to handle the click event when using 'and when clicked'.")
            return false
        }

        if (hasSection()) {
            if (buttonIdExpr != null) {
                Skript.warning(
                    "Both a button ID and a section were provided in the override slot expression. The button will be used and the section will be ignored."
                )
                return true
            }

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

            if (trigger == null) {
                Skript.error("Failed to load the section for handling icon interaction in expression: $this")
                return false
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

        val slots = slotsExpr.getAll(event).map { it.toInt() }
        if (slots.isEmpty()) {
            Skript.error("Slot cannot be null.")
            return walk(event, false)
        }

        val pages = pagesExpr?.getAll(event)?.map { it.toInt() } ?: listOf(menu.properties.defaultPage)
        if (pages.isEmpty()) {
            Skript.error("Page cannot be empty.")
            return walk(event, false)
        }

        if (pages.any { it !in 0..<menu.size }) {
            Skript.error("One or more page numbers are out of bounds for the menu.")
            return walk(event, false)
        }

        val button = buttonIdExpr?.getSingle(event)?.let { Button.buttons[it] }

        val item = button?.let { button.item } ?: itemExpr?.getSingle(event)

        if (button == null && buttonIdExpr != null) {
            Skript.error("Button with ID '${buttonIdExpr?.getSingle(event)}' not found.")
            return walk(event, false)
        }

        val clickHandler = MenuCallbackUtils.buildClickHandler(
            button = button,
            trigger = trigger,
            sourceEvent = event,
            runTrigger = { trig, ev -> walk(trig, ev) },
            onError = { e ->
                val id = menu.id ?: "<unnamed>"
                Skript.exception(
                    e,
                    Thread.currentThread(),
                    "Error occurred in a slot callback for menu $id. This callback was added when overriding slot $slots in page $pages to item $item."
                )
            }
        )

        clickHandler?.let {
            menu.overrideSlots(pages, slots, item, refreshFlag, clickHandler)
        } ?: menu.overrideSlots(pages, slots, item, refreshFlag)


        return walk(event, false)
    }


    override fun toString(event: Event?, debug: Boolean): String {
        val sb = StringBuilder("override slot ${slotsExpr.toString(event, debug)} ")

        pagesExpr?.let {
            sb.append("in page ${it.toString(event, debug)} ")
        }

        sb.append("of menu ${menuExpr?.toString(event, debug) ?: "event menu"} to ")

        itemExpr?.let {
            if (buttonIdExpr != null) {
                sb.append("item: ${it.toString(event, debug)} ")
            }
        }

        buttonIdExpr?.let {
            sb.append("button: ${it.toString(event, debug)} ")
        }

        if (refreshFlag) sb.append("and refresh ")

        if (hasSection()) {
            sb.append("when clicked")
        }

        return sb.toString()
    }
}
