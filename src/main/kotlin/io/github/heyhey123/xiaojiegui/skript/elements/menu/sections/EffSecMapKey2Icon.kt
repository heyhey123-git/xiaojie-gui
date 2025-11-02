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
import io.github.heyhey123.xiaojiegui.gui.menu.component.IconProducer
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

@Name("Map Key to Icon")
@Description(
    "Map a string key to an icon (item) in a menu.",
    "You can optionally provide a section to handle click events on the icon."
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
                        "to (single:(icon|item) %itemstack%|multiple:(icon|item)s %itemstacks%) " +
                        "[for [(menu|gui)] %-menu%] " +
                        "[on (single:page %number%|multiple:pages %numbers%)]" +
                        "[refresh:(and (refresh|update))] " +
                        "[when:(and when clicked)]"
            )
        }
    }

    private var trigger: TriggerItem? = null

    private lateinit var keyExpr: Expression<String>

    private lateinit var itemExpr: Expression<ItemStack>

    private var menuExpr: Expression<Menu>? = null

    private var pageExpr: Expression<Number>? = null

    private var refreshFlag: Boolean = false

    private lateinit var iconProducerType: IconProducer.Type

    private var pageTypeTag: String? = null

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
        itemExpr = expressions[1] as Expression<ItemStack>
        menuExpr = expressions[2] as Expression<Menu>?
        pageExpr = expressions[3] as Expression<Number>?

        if (parseResult!!.hasTag("refresh")) {
            refreshFlag = true
        }

        if (parseResult.hasTag("when") && !hasSection()) {
            Skript.error("You must provide a section to handle the click event when using 'and when clicked'.")
            return false
        }

        val iconProducerTypeTag = parseResult.tags.firstOrNull()
        if (iconProducerTypeTag == null) {
            Skript.error("Icon producer type could not be determined.")
            return false
        }
        iconProducerType = IconProducer.Type.valueOf(iconProducerTypeTag.uppercase())

        val pageTypeTag = parseResult.tags.getOrNull(1)
        this.pageTypeTag = pageTypeTag

        if (hasSection()) {
            val trigger = SectionUtils.loadLinkedCode(
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

            this.trigger = trigger ?: return false
        }

        return true
    }

    override fun walk(event: Event?): TriggerItem? {
        val key = keyExpr.getSingle(event)
        if (key == null) {
            Skript.error("Key cannot be null.")
            return walk(event, false)
        }

        val item = itemExpr.getSingle(event)
        if (item == null) {
            Skript.error("Item cannot be null.")
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

        val pages: List<Int>? = when (pageTypeTag) {
            "single" -> {
                val value = pageExpr!!.getSingle(event) ?: run {
                    Skript.error("Page number cannot be null.")
                    return walk(event, false)
                }
                listOf(value.toInt())
            }
            "multiple" -> pageExpr!!.getArray(event).map { it.toInt() }
            else -> null
        }?.also { ps ->
            // Validate page numbers
            for (p in ps) {
                if (p !in 0 until menu.size) {
                    Skript.error("Page number $p is out of bounds for menu with ${menu.size} pages.")
                    return walk(event, false)
                }
            }
        }

        val iconProducer = when (iconProducerType) {
            IconProducer.Type.SINGLE -> IconProducer.SingleIconProducer(item)
            IconProducer.Type.MULTIPLE -> {
                val items = itemExpr.getArray(event)
                IconProducer.MultipleIconProducer(items.toList())
            }
        }


            if (trigger == null) {
                menu.updateIconForKey(key, iconProducer, refreshFlag, pages)
                return walk(event, false)
            }

            menu.updateIconForKey(key, iconProducer, refreshFlag, pages) { menuEvent ->
                try {
                    Variables.withLocalVariables(event, menuEvent) {
                        walk(trigger, menuEvent)
                    }
                } catch (e: Throwable) {
                    val id = menu.id ?: "<unnamed>"

                    Skript.exception(
                        e,
                        Thread.currentThread(),
                        "Error occurred in slot callback for menu $id. This callback was added when mapping key $key to item $item."
                    )
                }
            }

        return walk(event, false)
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val keyStr = keyExpr.toString(event, debug)
        val itemStr = itemExpr.toString(event, debug)
        val menuStr = menuExpr?.toString(event, debug)
        val base = "map key $keyStr to item $itemStr for menu $menuStr"

        return if (hasSection()) "$base and when clicked do ..." else base
    }
}
