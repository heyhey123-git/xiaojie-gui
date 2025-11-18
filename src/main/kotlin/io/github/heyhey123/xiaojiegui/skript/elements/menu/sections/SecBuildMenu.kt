package io.github.heyhey123.xiaojiegui.skript.elements.menu.sections

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.config.SectionNode
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.*
import ch.njol.skript.lang.util.SectionUtils
import ch.njol.skript.variables.Variables
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuProperties
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.github.heyhey123.xiaojiegui.skript.elements.menu.event.ProvideMenuEvent
import io.github.heyhey123.xiaojiegui.skript.utils.ComponentHelper
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.Event
import org.bukkit.event.inventory.InventoryType
import org.skriptlang.skript.lang.entry.EntryValidator
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData
import org.skriptlang.skript.lang.entry.util.LiteralEntryData

@Name("Build Menu")
@Description(
    "Build a menu with specified properties.",
    "You can specify the menu type (phantom or static), inventory type, title, layout, id, default page, click delay, and whether to hide the player inventory.",
    "You can also provide an edit section to define additional properties or behaviors for the menu.",
    "The created menu is stored in the specified variable."
)
@Examples(
    "build a menu {_menu}",
    "    mode: phantom",
    "    inventory type: chest inventory",
    "    title: \"Main Menu\"",
    "    layout: \"AAA\", \"ABA\", \"AAA\"",
    "    id: \"main_menu\"",
    "    click delay: 100",
    "    hide player inventory: true",
    "    edit:",
    "        override slot 4 in page 0 to diamond named \"Special Item\""
)
@Since("1.0.3")
class SecBuildMenu : Section() {

    companion object {
        init {
            Skript.registerSection(
                SecBuildMenu::class.java,
                "build [a] menu [%-object%]"
            )
        }
    }

    private var menuVar: Variable<Any?>? = null

    private lateinit var mode: Receptacle.Mode

    private lateinit var inventoryTypeExpr: Expression<InventoryType>

    private lateinit var titleExpr: Expression<Any>

    private lateinit var layoutExpr: Expression<String>

    private var idExpr: Expression<String>? = null

    private var defaultPageExpr: Expression<Number>? = null

    private var clickDelayExpr: Expression<Number>? = null

    private var hidePlayerInventoryFlag: Boolean = false

    private var trigger: TriggerItem? = null

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?,
        sectionNode: SectionNode?,
        triggerItems: List<TriggerItem?>?
    ): Boolean {
        menuVar = expressions!![0] as Variable<Any?>?
        return parseNode(sectionNode!!)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseNode(sectionNode: SectionNode): Boolean {
        ComponentHelper.titleReturnTypes as Array<Class<Any>>

        val validator = EntryValidator.builder()
            .addEntryData(LiteralEntryData("mode", "phantom", true, String::class.java))
            .addEntryData(ExpressionEntryData("inventory type", null, false, InventoryType::class.java))
            .addEntryData(ExpressionEntryData("title", null, false, *ComponentHelper.titleReturnTypes))
            .addEntryData(ExpressionEntryData("layout", null, false, String::class.java))
            .addEntryData(ExpressionEntryData("id", null, true, String::class.java))
            .addEntryData(ExpressionEntryData("default page", null, true, Number::class.java))
            .addEntryData(ExpressionEntryData("click delay", null, true, Number::class.java))
            .addEntryData(LiteralEntryData("hide player inventory", false, true, Boolean::class.java))
            .addSection("edit", true)
            .build()

        val container = validator.validate(sectionNode)
        if (container == null) {
            Skript.error("Invalid menu section syntax. Please, check your syntax.")
            return false
        }

        // mode
        val modeStr = (container.getOptional("mode", true) as String?)?.lowercase()
        mode = when (modeStr) {
            "static" -> Receptacle.Mode.STATIC
            "phantom" -> Receptacle.Mode.PHANTOM
            else -> {
                Skript.error("Invalid menu mode: $modeStr. Must be 'phantom' or 'static'.")
                return false
            }
        }

        // inventory type
        val inventoryTypeData = container.getOptional("inventory type", false) as? Expression<InventoryType>
        if (inventoryTypeData == null) {
            Skript.error("Valid inventory type is required in create menu section.")
            return false
        }
        inventoryTypeExpr = inventoryTypeData

        // title
        val titleData = container.getOptional("title", false) as? Expression<Any>
        if (titleData == null) {
            Skript.error("Valid title is required in create menu section.")
            return false
        }
        titleExpr = titleData

        // layout
        val layoutData = container.getOptional("layout", false) as? Expression<String>
        if (layoutData == null) {
            Skript.error("Valid layout is required in create menu section.")
            return false
        }
        layoutExpr = layoutData

        // optional fields
        idExpr = container.getOptional("id", true) as? Expression<String>
        defaultPageExpr = container.getOptional("default page", false) as? Expression<Number>
        clickDelayExpr = container.getOptional("click delay", false) as? Expression<Number>

        val hidePlayerInventoryExpr = container.getOptional("hide player inventory", true) as? Literal<Boolean>
        hidePlayerInventoryFlag = hidePlayerInventoryExpr?.getSingle() ?: false

        // edit section
        var triggerNode: SectionNode? = null

        for (node in sectionNode) {
            if (node is SectionNode && "edit".equals(node.getKey(), ignoreCase = true)) {
                triggerNode = node
                break
            }
        }

        if (triggerNode != null) {
            trigger = SectionUtils.loadLinkedCode(
                "build menu"
            ) { beforeLoading: Runnable?, afterLoading: Runnable? ->
                loadCode(
                    triggerNode,
                    "build menu",
                    beforeLoading,
                    afterLoading,
                    ProvideMenuEvent::class.java
                )
            }
        }

        return true
    }

    override fun walk(event: Event?): TriggerItem? {
        val inventoryType = this.inventoryTypeExpr.getSingle(event)
        if (inventoryType == null) {
            Skript.error("Inventory type cannot be null.")
            return walk(event, false)
        }

        val title: Component
        val titleData = titleExpr.getSingle(event)
        if (titleData == null) {
            Skript.error("Menu title cannot be null.")
            return walk(event, false)
        }

        if (
            ComponentHelper.hasSkBee &&
            titleExpr.canReturn(ComponentHelper.skbeeComponentWrapper)
        ) {
            title = ComponentHelper.extractComponent(titleData)
        } else {
            val convertedTitle = titleExpr.getConvertedExpression(String::class.java)?.getSingle(event)
            if (convertedTitle == null) {
                Skript.error("The given menu title is not a textcomponent, and cannot be converted to string.")
                return walk(event, false)
            }
            title = LegacyComponentSerializer.legacySection().deserialize(convertedTitle)
        }

        val layout = layoutExpr.getArray(event)

        val id = idExpr?.getSingle(event)

        val defaultPage = defaultPageExpr?.getSingle(event)?.toInt()

        val clickDelay = clickDelayExpr?.getSingle(event)?.toInt()

        val properties = MenuProperties(
            defaultTitle = title,
            hidePlayerInventoryFlag,
            mode,
            minClickDelay = clickDelay ?: 5,
            defaultPage ?: 0,
            defaultLayout = layout.toList()
        )

        val menu = Menu(id, properties, inventoryType)

        if (defaultPage == null) {
            menu.insertPage(null, layout.toList(), title, null)
        }

        menuVar?.change(event, arrayOf(menu), Changer.ChangeMode.SET)
        val menuProvider = ProvideMenuEvent(menu)

        if (trigger != null) {
            Variables.withLocalVariables(event, menuProvider) {
                walk(trigger, menuProvider)
            }
        }

        return walk(event, false)
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val sb = StringBuilder("create ")
        sb.append(if (mode == Receptacle.Mode.PHANTOM) "a phantom" else "a static")
        sb.append(" menu of type ").append(inventoryTypeExpr.toString(event, debug))
        sb.append(" with title ").append(titleExpr.toString(event, debug))
        sb.append(" and layout ").append(layoutExpr.toString(event, debug))
        idExpr?.let {
            sb.append(", with id ").append(it.toString(event, debug))
        }
        defaultPageExpr?.let {
            sb.append(", default page ").append(it.toString(event, debug))
        }
        clickDelayExpr?.let {
            sb.append(", click delay ").append(it.toString(event, debug))
        }
        if (hidePlayerInventoryFlag) {
            sb.append(", hiding player inventory")
        }
        return sb.toString()
    }
}
