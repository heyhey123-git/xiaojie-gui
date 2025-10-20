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
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuProperties
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.github.heyhey123.xiaojiegui.skript.ComponentHelper
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import org.bukkit.event.Event
import org.bukkit.event.inventory.InventoryType

@Name("Create Menu")
@Description(
    "Create a menu.",
    "You can define the menu's properties, such as its inventory type, title, id, layout, page, click delay, and whether to hide the player's inventory.",
    "You can also define the menu's contents and behavior in the section below this effect."
)
@Examples(
    "create phantom menu with chest inventory titled \"Main Menu\" with id \"main_menu\" with layout \"xxxxxxxxx, xooooooxx, xxxxxxxox\" with page 0 with 500 ms click delay with hide player inventory:",
    "set slot 0 of menu to stone named \"Click me!\"",
    "set slot 1 of menu to dirt named \"No, click me!\"",
    "open menu for player"
)
@Since("1.0-SNAPSHOT")
class EffSecCreateMenu : EffectSection() {

    companion object {
        init {
            Skript.registerSection(
                EffSecCreateMenu::class.java,
                "create [a] [phantom|static] menu" +
                        "with %inventorytype% inventory " +
                        "titled %object% " +
                        "[with id %-string%] " +
                        "[with layout %-string%] " +
                        "[with page %-number%] " +
                        "[with %-number% ms click delay] " +
                        "[(with|without) hide player inventory]"
            )
        }
    }

    private var trigger: TriggerItem? = null

    private lateinit var mode: Receptacle.Mode

    private lateinit var inventoryType: Expression<InventoryType>

    private lateinit var title: Expression<Any>

    private var id: Expression<String>? = null

    private var layout: Expression<String>? = null

    private var page: Expression<Number>? = null

    private var minClickDelay: Expression<Number>? = null

    private var hidePlayerInventory: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?,
        sectionNode: SectionNode?,
        triggerItems: List<TriggerItem?>?
    ): Boolean {
        mode = if (parseResult!!.hasTag("static")) Receptacle.Mode.STATIC else Receptacle.Mode.PHANTOM
        inventoryType = expressions!![0] as Expression<InventoryType>
        title = expressions[1] as Expression<Any>
        id = expressions[2] as Expression<String>?
        layout = expressions[3] as Expression<String>?
        page = expressions[4] as Expression<Number>?
        minClickDelay = expressions[5] as Expression<Number>?
        hidePlayerInventory = !parseResult.hasTag("without")

        if (hasSection()) {
            val trigger = SectionUtils.loadLinkedCode(
                "create menu"
            ) { beforeLoading: Runnable?, afterLoading: Runnable? ->
                loadCode(
                    sectionNode,
                    "create menu",
                    beforeLoading,
                    afterLoading,
                    ProvideMenuEvent::class.java
                )
            }

            this.trigger = trigger ?: return false
        }

        return true
    }

    override fun walk(event: Event?): TriggerItem? {
        val inventoryType = this.inventoryType.getSingle(event)
            ?: return walk(event, false) // inventory type is required

        val defaultTitle = ComponentHelper.extractComponent(this.title, event)
            ?: return walk(event, false) // title is required

        val id = this.id?.getSingle(event)
        val defaultLayout = this.layout?.getAll(event)?.toList()
        val defaultPage = this.page?.getSingle(event)?.toInt()
        val minClickDelay = this.minClickDelay?.getSingle(event)?.toInt() ?: 0

        val properties = MenuProperties(
            defaultTitle,
            hidePlayerInventory,
            mode,
            minClickDelay,
            defaultPage ?: 0,
            defaultLayout ?: listOf()
        )

        val menu = Menu(id, properties, inventoryType)

        if (defaultLayout != null && defaultLayout.isNotEmpty() && defaultPage == null) {
            menu.insertPage(
                0,
                defaultLayout,
                defaultTitle,
                null
            )
        }

        if (trigger != null) {
            val menuProvider = ProvideMenuEvent(menu)
            Variables.withLocalVariables(event, menuProvider) {
                TriggerItem.walk(trigger, menuProvider)
            }
        }

        return walk(event, false)
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val str = StringBuilder("create ${mode.toString().lowercase()} menu with ")
            .append(inventoryType.toString(event, debug))
            .append(" inventory titled ")
            .append(title.toString(event, debug))

        id?.getSingle(event)?.let {
            str.append(" with id ")
                .append(it)
        }

        layout?.getAll(event)?.toList()?.let {
            if (it.isNotEmpty()) {
                str.append(" with layout ")
                    .append(it.joinToString(", "))
            }
        }

        page?.getSingle(event)?.let {
            str.append(" with page ")
                .append(it)
        }

        str.append(" with ")
            .append(minClickDelay?.toString(event, debug))
            .append(" ms click delay ")
            .append(if (hidePlayerInventory) "with" else "without")
            .append(" hide player inventory")

        return str.toString()
    }

}
