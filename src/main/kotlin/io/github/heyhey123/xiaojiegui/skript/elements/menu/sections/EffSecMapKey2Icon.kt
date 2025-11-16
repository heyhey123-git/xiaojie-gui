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
import io.github.heyhey123.xiaojiegui.gui.menu.component.IconProducer
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import io.github.heyhey123.xiaojiegui.skript.utils.Button
import io.github.heyhey123.xiaojiegui.skript.utils.MenuCallbackUtils
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

@Name("Map Key to Icon")
@Description(
    "Map a string key to an icon (item) in a menu.",
    "You can optionally provide a section to handle click events on the icon.",
    "Tips: If you didn't provide a section, the slot will simply be overridden without changing previous click behavior."
)
@Examples(
    "map key \"special_item\" to item diamond named \"Special Item\" for menu {_menu} and refresh and when clicked:",
    "    send \"You clicked the special item!\" to player"
)
@Since("1.0-SNAPSHOT")
class EffSecMapKey2Icon : EffectSection() {

    companion object {
        init {
            Skript.registerSection(
                EffSecMapKey2Icon::class.java,
                "map key %string% " +
                        "to ((icon|item)[s] %itemstacks%|button %-string%) " +
                        "[for [(menu|gui)] %-menu%] " +
                        "[on page(s) %-numbers%)]" +
                        "[refresh:(and (refresh|update))] " +
                        "[when:(and when clicked)]"
            )
        }
    }

    private var trigger: TriggerItem? = null

    private lateinit var keyExpr: Expression<String>

    private var itemExpr: Expression<ItemStack>? = null

    private var buttonIdExpr: Expression<String>? = null

    private var menuExpr: Expression<Menu>? = null

    private var pageExpr: Expression<Number>? = null

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
        keyExpr = expressions!![0] as Expression<String>
        itemExpr = expressions[1] as Expression<ItemStack>?
        buttonIdExpr = expressions[2] as Expression<String>?
        menuExpr = expressions[3] as Expression<Menu>?
        pageExpr = expressions[4] as Expression<Number>?

        if (parseResult!!.hasTag("refresh")) {
            refreshFlag = true
        }

        if (parseResult.hasTag("when") && !hasSection()) {
            Skript.error("You must provide a section to handle the click event when using 'and when clicked'.")
            return false
        }

        if (hasSection()) {
            if (buttonIdExpr != null) {
                Skript.warning("Both a button ID and a section were provided in the override slot expression. The button will be used and the section will be ignored.")
                return true
            }

            trigger = SectionUtils.loadLinkedCode(
                "interact with icon"
            ) { beforeLoading: Runnable?, afterLoading: Runnable? ->
                loadCode(
                    sectionNode,
                    "interact with icon",
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
        val key = keyExpr.getSingle(event)
        if (key == null) {
            Skript.error("Key cannot be null.")
            return walk(event, false)
        }

        val menu = menuExpr?.getSingle(event) ?: when (event) {
            is MenuEvent -> event.menu
            is ProvideMenuEvent -> event.menu
            else -> null
        }
        if (menu == null) {
            Skript.error("Menu cannot be null.")
            return walk(event, false)
        }

        val pages: List<Int>? = pageExpr?.getArray(event)?.map { it.toInt() }

        val button = buttonIdExpr?.getSingle(event)?.let { Button.buttons[it] }

        if (button == null && buttonIdExpr != null) {
            Skript.error("Button with ID '${buttonIdExpr!!.getSingle(event)}' not found.")
            return walk(event, false)
        }

        val items: Array<ItemStack> = button?.let { arrayOf(button.item) } ?: itemExpr?.getArray(event)!!

        val iconProducer: IconProducer = when (items.size) {
            0 -> {
                Skript.error("At least one item must be provided to map to an icon.")
                return walk(event, false)
            }

            1 -> IconProducer.SingleIconProducer(items.single())

            else -> IconProducer.MultipleIconProducer(items.toList())
        }

        val clickHandler = MenuCallbackUtils.buildClickHandler(
            button = button,
            trigger = trigger,
            sourceEvent = event,
            runTrigger = { trig, ev -> walk(trig, ev) },
            onError = { e ->
                val id = menu.id ?: "<unnamed>"
                val itemDesc = items.joinToString { it.type.name }
                Skript.exception(
                    e,
                    Thread.currentThread(),
                    "Error in icon callback for menu $id when mapping key '$key' to items [$itemDesc]."
                )
            }
        )

        clickHandler?.let {
            menu.updateIconForKey(key, iconProducer, refreshFlag, pages, clickHandler)
        } ?: menu.updateIconForKey(key, iconProducer, refreshFlag, pages)

        return walk(event, false)
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val sb = StringBuilder("map key ${keyExpr.toString(event, debug)} to ")

        buttonIdExpr?.let {
            sb.append("button ${it.toString(event, debug)} ")
        } ?: run {
            sb.append("item ${itemExpr!!.toString(event, debug)} ")
        }

        sb.append("for menu ${menuExpr?.toString(event, debug) ?: "event menu"} ")

        pageExpr?.let {
            sb.append("on page ${it.toString(event, debug)} ")
        }

        if (refreshFlag) {
            sb.append("and refresh ")
        }

        trigger?.let {
            sb.append("when clicked")
        }

        return sb.toString()
    }
}
