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
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuProperties
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.github.heyhey123.xiaojiegui.skript.ComponentHelper
import io.github.heyhey123.xiaojiegui.skript.TitleType
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import net.kyori.adventure.text.Component
import org.bukkit.event.Event
import org.bukkit.event.inventory.InventoryType

@Name("Create Menu")
@Description(
    "Create a menu.",
    "You can define the menu's properties, such as its inventory type, title, id, layout, page, click delay, and whether to hide the player's inventory.",
    "You can also define the menu's contents and behavior in the section below this effect.",
    "Tips: If you do not specify a default page, the menu will insert a page 0 with the given layout and title."
)
@Examples(
    "create a static menu with chest inventory titled \"Main Menu\" with id \"main_menu\" with layout \"AAA\", \"ABA\", \"AAA\" with 100 ms click delay with hide player inventory:",
    "    set slot 4 in page 0 of menu with id \"main_menu\" to diamond named \"Special Item\""
)
@Since("1.0-SNAPSHOT")
class EffSecCreateMenu : EffectSection() {

    companion object {
        init {
            Skript.registerSection(
                EffSecCreateMenu::class.java,
                "create [a] [:phantom|:static] menu " +
                        "with %inventorytype% " +
                        "titled (string:%-string%|component:%-textcomponent%) " +
                        "with layout %strings% " +
                        "[with id %-string%] " +
                        "[with page %-number%] " +
                        "[with %-number% ms click delay] " +
                        "[(hide:with|without) hide player inventory)]"
            )
        }
    }

    private var trigger: TriggerItem? = null

    private lateinit var mode: Receptacle.Mode

    private lateinit var inventoryTypeExpr: Expression<InventoryType>

    private var titleStrExpr: Expression<String>? = null

    private var titleComponentExpr: Expression<Any>? = null

    private lateinit var titleType: TitleType

    private lateinit var layoutExpr: Expression<String>

    private var idExpr: Expression<String>? = null

    private var pageExpr: Expression<Number>? = null

    private var minClickDelayExpr: Expression<Number>? = null

    private var hidePlayerInventoryFlag: Boolean = false

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
        inventoryTypeExpr = expressions!![0] as Expression<InventoryType>
        titleStrExpr = expressions[1] as Expression<String>?
        titleComponentExpr = expressions[2] as Expression<Any>?
        titleType = TitleType.fromStringTag(parseResult.tags[1])
        idExpr = expressions[3] as Expression<String>?
        layoutExpr = expressions[4] as Expression<String>
        pageExpr = expressions[5] as Expression<Number>?
        minClickDelayExpr = expressions[6] as Expression<Number>?
        hidePlayerInventoryFlag = parseResult.hasTag("hide")

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
        val inventoryType = this.inventoryTypeExpr.getSingle(event)
        if (inventoryType == null) {
            Skript.error("Inventory type is required.")
            return walk(event, false)
        }

        val defaultTitle: Component? = ComponentHelper.resolveTitleComponentOrNull(
            titleStrExpr,
            titleComponentExpr,
            event,
            titleType,
        )

        if (defaultTitle == null) {
            Skript.error("Valid Menu title is required.")
            return walk(event, false)
        }// title is required

        val defaultLayout = this.layoutExpr.getAll(event)?.toList()
        val id = this.idExpr?.getSingle(event)
        val defaultPage = this.pageExpr?.getSingle(event)?.toInt()
        val minClickDelay = this.minClickDelayExpr?.getSingle(event)?.toInt() ?: 0

        val properties = MenuProperties(
            defaultTitle,
            hidePlayerInventoryFlag,
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
                walk(trigger, menuProvider)
            }
        }

        return walk(event, false)
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val str = StringBuilder("create ${mode.toString().lowercase()} menu with ")
            .append(inventoryTypeExpr.toString(event, debug))
            .append(" inventory titled ")
            .append((titleStrExpr ?: titleComponentExpr)?.toString(event, debug))

        layoutExpr.getAll(event)?.toList()?.let {
            if (it.isNotEmpty()) {
                str.append(" with layout ")
                    .append(it.joinToString(", "))
            }
        }

        idExpr?.getSingle(event)?.let {
            str.append(" with id ")
                .append(it)
        }

        pageExpr?.getSingle(event)?.let {
            str.append(" with page ")
                .append(it)
        }

        str.append(" with ")
            .append(minClickDelayExpr?.toString(event, debug))
            .append(" ms click delay ")
            .append(if (hidePlayerInventoryFlag) "with" else "without")
            .append(" hide player inventory")

        return str.toString()
    }

}
