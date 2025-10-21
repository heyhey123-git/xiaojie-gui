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
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

@Name("Map Key to Icon")
@Description(
    "Map a string key to an icon (item) in a menu.",
    "You can optionally provide a section to handle click events on the icon."
)
@Examples(
    "map key \"close_button\" to item barrier named \"Close\" for menu {_menu} and when clicked:",
    "close the menu of player",
    "send \"Menu closed!\" to player"
)
@Since("1.0-SNAPSHOT")
class EffSecMapKey2Icon : EffectSection() {

    companion object {
        init {
            Skript.registerSection(
                EffSecMapKey2Icon::class.java,
                "map key %string% " +
                        "to (icon|item) %itemstack% " +
                        "for [(menu|gui)] %menu% " +
                        "[and (refresh|update)] " +
                        "[and when clicked]"
            )
        }
    }

    private var trigger: TriggerItem? = null

    private lateinit var key: Expression<String>

    private lateinit var item: Expression<ItemStack>

    private lateinit var menu: Expression<Menu>

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
        key = expressions!![0] as Expression<String>
        item = expressions[1] as Expression<ItemStack>
        menu = expressions[2] as Expression<Menu>

        if (parseResult!!.hasTag("refresh")) {
            refresh = true
        }

        if (parseResult.hasTag("when") && !hasSection()) {
            Skript.error("You must provide a section to handle the click event when using 'and when clicked'.")
            return false
        }

        if (hasSection()) {
            val trigger = SectionUtils.loadLinkedCode(
                "interact with icon"
            ) { beforeLoading: Runnable?, afterLoading: Runnable? ->
                loadCode(
                    sectionNode,
                    "interact with icon",
                    beforeLoading,
                    afterLoading,
                    ReceptacleInteractEvent::class.java
                )
            }

            this.trigger = trigger ?: return false
        }

        return true
    }

    override fun walk(event: Event?): TriggerItem? {
        val key = key.getSingle(event)
        val item = item.getSingle(event)
        val menu = menu.getSingle(event)

        if (key == null) {
            Skript.error("Key cannot be null.")
            return walk(event, false)
        }

        if (item == null) {
            Skript.error("Item cannot be null.")
            return walk(event, false)
        }

        if (menu == null) {
            Skript.error("Menu cannot be null.")
            return walk(event, false)
        }

        if (trigger == null) {
            menu.updateIconForKey(key, item, refresh)
            return walk(event, false)
        }

        menu.updateIconForKey(key, item, refresh) { menuEvent ->
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
        val keyStr = key.toString(event, debug)
        val itemStr = item.toString(event, debug)
        val menuStr = menu.toString(event, debug)
        val base = "map key $keyStr to item $itemStr for menu $menuStr"

        return if (hasSection()) "$base and when clicked do ..." else base
    }
}
