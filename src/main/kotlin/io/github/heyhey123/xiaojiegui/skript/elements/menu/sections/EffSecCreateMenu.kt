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
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import io.github.heyhey123.xiaojiegui.skript.utils.ComponentHelper
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import net.kyori.adventure.text.Component
import org.bukkit.event.Event
import org.bukkit.event.inventory.InventoryType
import org.skriptlang.skript.addon.SkriptAddon

@Name("Create Menu")
@Description(
    "Create a menu.",
    "You can define the menu's properties, such as its inventory type, title, id, layout, page, click delay, and whether to hide the player's inventory.",
    "You can also define the menu's contents and behavior in the section below this effect.",
    "Tips: The layout always becomes the first page. `with page N` only decides which page `open menu` shows.",
    "If you create a menu with a id that already exists, the old one will be destroyed."
)
@Examples(
    "create a static menu with chest inventory titled \"Main Menu\" with id \"main_menu\" with layout \"AAA\", \"ABA\", \"AAA\" with 100 ms click delay with hide player inventory:",
    // Filling a slot is `override slot ... for menu ...`; `set slot 4 in page 1 of menu with id ...`
    // is not this addon's syntax and parses as neither an effect nor a condition.
    "    override slot 4 in page 1 to diamond named \"Special Item\" for menu with id \"main_menu\""
)
@Since("1.0.0")
class EffSecCreateMenu : EffectSection() {

    companion object {
        fun register(addon: SkriptAddon) {
            SkriptSyntax.section(
                addon,
                EffSecCreateMenu::class.java,
                // One pattern per title form instead of one group with two alternatives: when the
                // alternatives of a group have different types, Skript hands the literal over unparsed,
                // and reading it then throws "UnparsedLiterals must be converted before use" at runtime.
                // A pattern of its own gives the slot a single type, which Skript parses up front.
                "create [a] [:phantom|:static] menu " +
                    "with %inventorytype% " +
                    "titled string:%-string% " +
                    "with layout %strings% " +
                    "[with id %-string%] " +
                    "[with page %-number%] " +
                    "[with %-number% ms click delay] " +
                    "[(hide:with|without) hide player inventory]",
                "create [a] [:phantom|:static] menu " +
                    "with %inventorytype% " +
                    "titled %-object% " +
                    "with layout %strings% " +
                    "[with id %-string%] " +
                    "[with page %-number%] " +
                    "[with %-number% ms click delay] " +
                    "[(hide:with|without) hide player inventory]"
            )
        }
    }

    private var trigger: TriggerItem? = null

    private lateinit var mode: Receptacle.Mode

    private lateinit var inventoryTypeExpr: Expression<InventoryType>

    private var titleExpr: Expression<Any>? = null

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
        titleExpr = expressions[1] as Expression<Any>?
        layoutExpr = expressions[2] as Expression<String>
        idExpr = expressions[3] as Expression<String>?
        pageExpr = expressions[4] as Expression<Number>?
        minClickDelayExpr = expressions[5] as Expression<Number>?
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

        val defaultTitle: Component? = ComponentHelper.resolveTitleComponentOrNull(titleExpr, event)

        if (defaultTitle == null) {
            Skript.error("Valid Menu title is required.")
            return walk(event, false)
        } // title is required

        val defaultLayout = this.layoutExpr.getAll(event)?.toList()
        val id = this.idExpr?.getSingle(event)
        val defaultPage = this.pageExpr?.getSingle(event)?.toInt()
        val minClickDelay = this.minClickDelayExpr?.getSingle(event)?.toInt()

        val properties = MenuProperties(
            defaultTitle,
            hidePlayerInventoryFlag,
            mode,
            minClickDelay ?: 50,
            defaultPage ?: 1,
            defaultLayout ?: listOf()
        )

        val menu = Menu(id, properties, inventoryType)

        // The given layout always becomes page 1; `with page N` only decides which page `open menu` shows.
        // Making this conditional left a menu with no pages at all whenever the user set a page, which
        // `open menu` then refused.
        if (defaultLayout != null && defaultLayout.isNotEmpty()) {
            menu.insertPage(null, defaultLayout, defaultTitle, null)
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
        val str = StringBuilder("create ${mode.id} menu with ")
            .append(inventoryTypeExpr.toString(event, debug))
            .append(" inventory titled ")
            .append((titleExpr)?.toString(event, debug))

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
