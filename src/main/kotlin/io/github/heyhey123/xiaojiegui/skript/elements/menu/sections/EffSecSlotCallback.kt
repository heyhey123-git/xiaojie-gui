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
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import org.bukkit.event.Event


@Name("Set Slot Callback")
@Description(
    "Set a callback for a specific slot in a specific page of a menu.",
    "You must provide a section to handle click events on the specified slot."
)
@Examples(
    "when slot 4 in page 0 of menu with id \"main_menu\" is clicked:",
    "    send \"You clicked slot 4!\" to player"
)
@Since("1.0-SNAPSHOT")
class EffSecSlotCallback : EffectSection() {

    companion object {
        init {
            Skript.registerSection(
                EffSecSlotCallback::class.java,
                "(when|on) slot %numbers% " +
                        "[in page [(number|index)] %-numbers%] " +
                        "[of [(menu|gui)] %-menu%] " +
                        "[is] (clicked|interacted|pressed)"
            )
        }
    }

    private var trigger: TriggerItem? = null

    private lateinit var slotsExpr: Expression<Number>

    private var pagesExpr: Expression<Number>? = null

    private var menuExpr: Expression<Menu>? = null

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
        menuExpr = expressions[2] as Expression<Menu>?

        if (!hasSection()) {
            Skript.error("You must provide a section to handle the slot click event.")
            return false
        }

        trigger = SectionUtils.loadLinkedCode(
            "slot callback"
        ) { beforeLoading: Runnable?, afterLoading: Runnable? ->
            loadCode(
                sectionNode,
                "create menu",
                beforeLoading,
                afterLoading,
                MenuInteractEvent::class.java
            )
        }

        this.trigger = trigger ?: return false

        return true
    }

    override fun walk(event: Event?): TriggerItem? {
        if (!hasSection()) {
            Skript.error("No section found to handle the slot callback.")
            return walk(event, false)
        }

        val menu = this.menuExpr?.getSingle(event) ?: when (event) {
            is MenuInteractEvent -> event.menu
            else -> null
        }
        if (menu == null) {
            Skript.error("Failed to get the menu to set slot callback. Please check your code.")
            return walk(event, false)
        }

        val slots = this.slotsExpr.getAll(event).map { it.toInt() }
        val pages = this.pagesExpr?.getAll(event)?.map { it.toInt() } ?: listOf(menu.properties.defaultPage)

        if (slots.isEmpty()) {
            Skript.error("Slot cannot be empty.")
            return walk(event, false)
        }

        if (pages.isEmpty()) {
            Skript.error("Page cannot be empty.")
            return walk(event, false)
        }

        for (singlePage in pages) {
            for (singleSlot in slots) {
                menu.setSlotCallback(
                    singlePage,
                    singleSlot
                ) { menuEvent: MenuInteractEvent ->
                    Variables.withLocalVariables(event, menuEvent) {
                        walk(trigger, menuEvent)
                    }
                }
            }
        }

        return walk(event, false)
    }

    override fun toString(event: Event?, debug: Boolean) =
        "set slot callback for slot ${slotsExpr.toString(event, debug)} in page ${
            pagesExpr?.toString(
                event,
                debug
            )
        } of menu ${menuExpr?.toString(event, debug)}"

}
