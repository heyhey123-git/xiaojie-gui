package io.github.heyhey123.xiaojiegui.skript.elements.button.sections

import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.lang.util.SectionUtils
import ch.njol.util.Kleenean
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.skript.utils.Button
import io.github.heyhey123.xiaojiegui.skript.utils.LocalsScopeRunner
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import org.skriptlang.skript.lang.entry.EntryValidator
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData

@Name("Define Button")
@Description(
    "Defines a button with the specified ID and icon.",
    "You must provide a 'when clicked' section to handle button click events.",
    "If a button with the same ID already exists, the new definition will overwrite the previous one."
)
@Examples(
    "define button \"my_button\":",
    "    icon: stone",
    "    when clicked:",
    "        send \"You clicked the button!\" to player"
)
@Since("1.0.4")
class SecDefineButton : Section() {
    companion object {
        init {
            Skript.registerSection(
                SecDefineButton::class.java,
                "define [a] button %string%"
            )
        }
    }

    lateinit var idExpr: Expression<String>

    lateinit var iconExpr: Expression<ItemStack>

    lateinit var trigger: TriggerItem

    @Suppress("UNCHECKED_CAST")
    override fun init(
        expressions: Array<out Expression<*>?>?,
        matchedPattern: Int,
        isDelayed: Kleenean?,
        parseResult: SkriptParser.ParseResult?,
        sectionNode: SectionNode?,
        triggerItems: List<TriggerItem?>?
    ): Boolean {
        idExpr = expressions!![0] as Expression<String>
        return parseNode(sectionNode!!)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseNode(sectionNode: SectionNode): Boolean {
        val validator = EntryValidator.builder()
            .addEntryData(ExpressionEntryData("icon", null, false, ItemStack::class.java))
            .addSection("when clicked", false)
            .build()

        val container = validator.validate(sectionNode)
        if (container == null) {
            Skript.error("Invalid syntax in register button section. Please, check your code.")
            return false
        }

        val icon = container.getOptional("icon", false) as? Expression<ItemStack>
        if (icon == null) {
            Skript.error("Icon expression is required in register button section.")
            return false
        }
        iconExpr = icon

        val callbackSection = sectionNode.get("when clicked") as? SectionNode
        if (callbackSection == null) {
            Skript.error("You must provide a 'when clicked' section to handle button click events.")
            return false
        }

        val callbackTrigger = SectionUtils.loadLinkedCode(
            "register button"
        ) { beforeLoading: Runnable?, afterLoading: Runnable? ->
            loadCode(
                callbackSection,
                "register button",
                beforeLoading,
                afterLoading,
                MenuInteractEvent::class.java
            )
        }
        if (callbackTrigger == null) {
            Skript.error("Failed to load the 'when clicked' section for register button.")
            return false
        }
        trigger = callbackTrigger

        return true
    }

    override fun walk(event: Event?): TriggerItem? {
        val id = idExpr.getSingle(event)
        if (id == null) {
            Skript.error("Failed to get the button ID in register button section.")
            return walk(event, false)
        }

        val icon = iconExpr.getSingle(event)
        if (icon == null) {
            Skript.error("Failed to get the button icon in register button section.")
            return walk(event, false)
        }

        val executor = LocalsScopeRunner(event) { menuEvent ->
            walk(trigger, menuEvent)
        }

        Button(id, icon) { event -> executor(event) }

        return walk(event, false)
    }

    override fun toString(event: Event?, debug: Boolean): String {
        val sb = StringBuilder("register button ")
        sb.append(idExpr.toString(event, debug))
        sb.append(" with icon ")
        sb.append(iconExpr.toString(event, debug))
        return sb.toString()
    }
}
